/* Copyright (c) 2016 JetRS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs.server;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.ProviderResource;
import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.provider.ext.header.CompatibleMediaType;
import org.jetrs.provider.ext.header.MediaTypes;
import org.jetrs.provider.ext.header.ServerMediaType;
import org.jetrs.server.container.ContainerRequestContextImpl;
import org.libj.lang.Identifiers;
import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManifest implements Comparable<ResourceManifest> {
  private static final Logger logger = LoggerFactory.getLogger(ResourceManifest.class);
  private static final PermitAll permitAll = new PermitAll() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return getClass();
    }
  };

  private static boolean logMissingHeaderWarning(final String headerName, final Class<?> type) {
    logger.warn("Unmatched @" + type.getSimpleName() + " for " + headerName);
    return false;
  }

  private static Annotation findSecurityAnnotation(final Method method) {
    final Annotation annotation = findSecurityAnnotation(method.getAnnotations());
    return annotation != null ? annotation : findSecurityAnnotation(method.getDeclaringClass().getAnnotations());
  }

  private static Annotation findSecurityAnnotation(final Annotation ... annotations) {
    for (final Annotation annotation : annotations)
      if (annotation.annotationType() == PermitAll.class || annotation.annotationType() == DenyAll.class || annotation.annotationType() == RolesAllowed.class)
        return annotation;

    return null;
  }

  private final HttpMethod httpMethod;
  private final Annotation securityAnnotation;
  private final Method method;
  private final Object singleton;
  private final Class<?> serviceClass;
  private final PathPattern pathPattern;
  private final MediaTypeAnnotationProcessor<Consumes> consumesMatcher;
  private final MediaTypeAnnotationProcessor<Produces> producesMatcher;

  ResourceManifest(final HttpMethod httpMethod, final Method method, final Path classPath, final Path methodPath, final Object singleton) {
    this.httpMethod = httpMethod;
    final Annotation securityAnnotation = findSecurityAnnotation(method);
    this.securityAnnotation = securityAnnotation != null ? securityAnnotation : permitAll;
    this.method = method;
    this.singleton = singleton;
    this.serviceClass = singleton != null ? singleton.getClass() : method.getDeclaringClass();
    this.pathPattern = new PathPattern(classPath, methodPath);
    this.consumesMatcher = new MediaTypeAnnotationProcessor<>(method, Consumes.class);
    this.producesMatcher = new MediaTypeAnnotationProcessor<>(method, Produces.class);
  }

  Object getResource() {
    return singleton != null ? singleton : serviceClass;
  }

  Annotation[] getMethodAnnotations() {
    return method.getAnnotations();
  }

  Class<?> getMethodReturnClass() {
    return method.getReturnType();
  }

  Type getMethodReturnType() {
    return method.getGenericReturnType();
  }

  CompatibleMediaType[] getCompatibleContentType(final MediaType mediaType, final List<String> acceptCharsets) {
    return consumesMatcher.getCompatibleMediaType(mediaType, acceptCharsets);
  }

  CompatibleMediaType[] getCompatibleAccept(final List<MediaType> acceptMediaTypes, final List<String> acceptCharsets) {
    return producesMatcher.getCompatibleMediaType(acceptMediaTypes, acceptCharsets);
  }

  @SuppressWarnings("rawtypes")
  private Object[] getParameters(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IOException {
    final Parameter[] parameters = method.getParameters();
    final Type[] genericParameterTypes = method.getGenericParameterTypes();
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    if (parameters.length == 0)
      return ArrayUtil.EMPTY_ARRAY;

    final Object[] parameterInstances = new Object[parameters.length];
    for (int i = 0; i < parameters.length; ++i) {
      final Parameter parameter = parameters[i];
      final Type genericParameterType = genericParameterTypes[i];
      final Annotation[] annotations = parameterAnnotations[i];
      final Annotation paramAnnotation = AnnotationInjector.getInjectableAnnotation(parameter, annotations);
      if (paramAnnotation == null) {
        final Providers providers = annotationInjector.getContextObject(Providers.class);
        final MessageBodyReader messageBodyReader = providers.getMessageBodyReader(parameter.getType(), genericParameterType, annotations, containerRequestContext.getMediaType());
        if (messageBodyReader == null)
          throw new WebApplicationException("Could not find MessageBodyReader for type: " + parameter.getType().getName());

        // FIXME: Why is there a return type for ReaderInterceptorContext#proceed()? And it's of type Object. What type is ReaderInterceptorContext supposed to return? It should be InputStream, but then it makes it redundant.
        containerRequestContext.setType(parameter.getType());
        containerRequestContext.setGenericType(parameter.getType().getGenericSuperclass());
        containerRequestContext.setAnnotations(parameter.getAnnotations());
        parameterInstances[i] = containerRequestContext.readBody(messageBodyReader);
      }
      else {
        parameterInstances[i] = annotationInjector.getParamObject(paramAnnotation, parameter.getType(), annotations, genericParameterType, paramConverterProviders);
        if (parameterInstances[i] instanceof Exception)
          throw new BadRequestException((Exception)parameterInstances[i]);
      }
    }

    return parameterInstances;
  }

  protected boolean checkContentHeader(final String headerName, final Class<? extends Annotation> annotationClass, final ContainerRequestContext containerRequestContext) {
    final Annotation annotation = getResourceAnnotationProcessor(annotationClass).getAnnotation();
    if (annotation == null) {
      final String message = "@" + annotationClass.getSimpleName() + " annotation missing for " + method.getDeclaringClass().getName() + "." + Identifiers.toClassCase(containerRequestContext.getMethod().toLowerCase()) + "()";
      if (annotationClass == Consumes.class)
        throw new IllegalStateException(message);

      logger.warn(message);
      return true;
    }

    final String headerValue = containerRequestContext.getHeaderString(headerName);
    if (headerValue == null || headerValue.length() == 0)
      return logMissingHeaderWarning(headerName, annotationClass);

    final String[] annotationValue = annotationClass == Produces.class ? ((Produces)annotation).value() : annotationClass == Consumes.class ? ((Consumes)annotation).value() : null;
    final ServerMediaType[] required = ServerMediaType.valueOf(annotationValue);
    final MediaType[] mediaTypes = MediaTypes.parse(headerValue.split(","));
    if (MediaTypes.getCompatible(required, mediaTypes, null) != null)
      return true;

    return logMissingHeaderWarning(headerName, annotationClass);
  }

  private void checkAllowed(final ContainerRequestContext containerRequestContext) {
    if (securityAnnotation instanceof PermitAll)
      return;

    if (securityAnnotation instanceof DenyAll)
      throw new ForbiddenException("@DenyAll");

    if (!(securityAnnotation instanceof RolesAllowed))
      throw new UnsupportedOperationException("Unsupported security annotation: " + securityAnnotation.getClass().getName());

    final RolesAllowed rolesAllowed = (RolesAllowed)securityAnnotation;
    if (containerRequestContext.getSecurityContext().getUserPrincipal() != null)
      for (final String role : rolesAllowed.value())
        if (containerRequestContext.getSecurityContext().isUserInRole(role))
          return;

    final String authenticationScheme = containerRequestContext.getSecurityContext().getAuthenticationScheme();
    final StringBuilder builder = new StringBuilder();
    if (authenticationScheme != null)
      builder.append(authenticationScheme).append(' ');

    // FIXME: What about "Proxy-Authenticate"?
    final String[] roles = rolesAllowed.value();
    builder.append("realm=\"").append(roles[0]).append('"');
    final String challenge = builder.toString();
    if (roles.length == 1)
      throw new NotAuthorizedException(challenge);

    final Object[] challenges = new Object[roles.length - 1];
    final int resetLen = authenticationScheme != null ? authenticationScheme.length() + 1 : 0;
    for (int i = 1; i < roles.length; ++i) {
      builder.setLength(resetLen);
      builder.append("realm=\"").append(roles[i]).append('"');
      challenges[i - 1] = builder.toString();
    }

    throw new NotAuthorizedException(challenge, challenges);
  }

  Object service(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IOException, ServletException {
    checkAllowed(containerRequestContext);
    try {
      return method.invoke(getResource(), getParameters(containerRequestContext, annotationInjector, paramConverterProviders));
    }
    catch (final IllegalAccessException e) {
      throw new ServletException(e);
    }
    catch (final InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException)e.getCause();

      if (e.getCause() instanceof IOException)
        throw (IOException)e.getCause();

      if (e.getCause() instanceof ServletException)
        throw (ServletException)e.getCause();

      throw new ServletException(e.getCause());
    }
    catch (final IllegalArgumentException e) {
      throw new BadRequestException(e);
    }
  }

  HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public PathPattern getPathPattern() {
    return pathPattern;
  }

  @SuppressWarnings("unchecked")
  <T extends Annotation>MediaTypeAnnotationProcessor<T> getResourceAnnotationProcessor(final Class<T> annotationClass) {
    return annotationClass == Consumes.class ? (MediaTypeAnnotationProcessor<T>)consumesMatcher : annotationClass == Produces.class ? (MediaTypeAnnotationProcessor<T>)producesMatcher : null;
  }

  boolean isRestricted() {
    return securityAnnotation instanceof DenyAll || securityAnnotation instanceof RolesAllowed;
  }

  @Override
  public int compareTo(final ResourceManifest o) {
    final int c = pathPattern.compareTo(o.pathPattern);
    if (c != 0)
      return c;

    // [JAX-RS 2.1 6.7.2 1.f]
    if (httpMethod == null)
      return o.httpMethod != null ? 1 : 0;

    return o.httpMethod == null ? -1 : 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ResourceManifest))
      return false;

    final ResourceManifest that = (ResourceManifest)obj;
    return Objects.equals(httpMethod, that.httpMethod) && securityAnnotation.equals(that.securityAnnotation) && method.equals(that.method) && serviceClass.equals(that.serviceClass) && pathPattern.equals(that.pathPattern) && consumesMatcher.equals(that.consumesMatcher) && producesMatcher.equals(that.producesMatcher);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    hashCode = 31 * hashCode + Objects.hashCode(httpMethod);
    hashCode = 31 * hashCode + securityAnnotation.hashCode();
    hashCode = 31 * hashCode + method.hashCode();
    hashCode = 31 * hashCode + serviceClass.hashCode();
    hashCode = 31 * hashCode + pathPattern.hashCode();
    hashCode = 31 * hashCode + consumesMatcher.hashCode();
    hashCode = 31 * hashCode + producesMatcher.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return (httpMethod != null ? httpMethod.value() : "*") + " " + pathPattern;
  }
}