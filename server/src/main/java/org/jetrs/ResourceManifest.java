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

package org.jetrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResourceManifest implements ResourceInfo, Comparable<ResourceManifest> {
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
  private final Method resourceMethod;
  private final Class<?> resourceClass;
  private final Object singleton;
  private final UriTemplate uriTemplate;
  private final MediaTypeAnnotationProcessor<Consumes> consumesMatcher;
  private final MediaTypeAnnotationProcessor<Produces> producesMatcher;

  ResourceManifest(final HttpMethod httpMethod, final Method method, final String baseUri, final Path classPath, final Path methodPath, final Object singleton) {
    this.httpMethod = httpMethod;
    final Annotation securityAnnotation = findSecurityAnnotation(method);
    this.securityAnnotation = securityAnnotation != null ? securityAnnotation : permitAll;
    this.resourceMethod = method;
    this.resourceClass = method.getDeclaringClass();
    this.singleton = singleton;
    if (singleton != null) {
      final Field[] fields = AnnotationInjector.getContextFields(singleton);
      if (fields.length > 0)
        logger.warn("Fields with @Context annotation " + Arrays.toString(fields) + " will not be injected on singleton of class " + resourceClass.getName());
    }

    this.uriTemplate = new UriTemplate(baseUri, classPath, methodPath);
    this.consumesMatcher = new MediaTypeAnnotationProcessor<>(method, Consumes.class);
    this.producesMatcher = new MediaTypeAnnotationProcessor<>(method, Produces.class);
  }

  @Override
  public Method getResourceMethod() {
    return this.resourceMethod;
  }

  @Override
  public Class<?> getResourceClass() {
    return this.resourceClass;
  }

  Object getSingleton() {
    return this.singleton;
  }

  Annotation[] getMethodAnnotations() {
    return resourceMethod.getAnnotations();
  }

  Class<?> getMethodReturnClass() {
    return resourceMethod.getReturnType();
  }

  Type getMethodReturnType() {
    return resourceMethod.getGenericReturnType();
  }

  CompatibleMediaType[] getCompatibleContentType(final MediaType mediaType, final List<String> acceptCharsets) {
    return consumesMatcher.getCompatibleMediaType(mediaType, acceptCharsets);
  }

  CompatibleMediaType[] getCompatibleAccept(final List<MediaType> acceptMediaTypes, final List<String> acceptCharsets) {
    return producesMatcher.getCompatibleMediaType(acceptMediaTypes, acceptCharsets);
  }

  @SuppressWarnings("rawtypes")
  private Object[] getParameters(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IOException {
    final Parameter[] parameters = resourceMethod.getParameters();
    final Type[] genericParameterTypes = resourceMethod.getGenericParameterTypes();
    final Annotation[][] parameterAnnotations = resourceMethod.getParameterAnnotations();
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
        parameterInstances[i] = containerRequestContext.readBody(annotationInjector, messageBodyReader);
      }
      else {
        parameterInstances[i] = getParamObject(containerRequestContext, annotationInjector, paramAnnotation, parameter.getType(), annotations, genericParameterType, paramConverterProviders);
        if (parameterInstances[i] instanceof Exception)
          throw new BadRequestException((Exception)parameterInstances[i]);
      }
    }

    return parameterInstances;
  }

  private Object getParamObject(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final Annotation annotation, final Class<?> parameterType, final Annotation[] annotations, final Type genericParameterType, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) {
    if (annotation.annotationType() == QueryParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      return ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, containerRequestContext.getUriInfo().getQueryParameters(decode).get(((QueryParam)annotation).value()), paramConverterProviders);
    }

    if (annotation.annotationType() == PathParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final String pathParam = ((PathParam)annotation).value();
      final MultivaluedMap<String,String> pathParameters = containerRequestContext.getUriInfo().getPathParameters(decode);
      final List<String> values = pathParameters.get(pathParam);
      // FIXME: Another useful warning would be: notify if more than 1 @PathParam annotations specify the same name
      if (values == null)
        logger.warn("@PathParam(\"" + pathParam + "\") not found in URI template of @Path(\"" + uriTemplate + "\") on method: " + resourceMethod.getDeclaringClass().getName() + "." + resourceMethod.getName() + "(" + ArrayUtil.toString(resourceMethod.getParameterTypes(), ',', Class::getName) + ")");

      return ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, values, paramConverterProviders);
    }

    if (annotation.annotationType() == MatrixParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final List<PathSegment> pathSegments = containerRequestContext.getUriInfo().getPathSegments(decode);
      // FIXME: Is it the last PathSegment that from which to get the matrix?
      final PathSegment pathSegment = pathSegments.get(pathSegments.size() - 1);
      final MultivaluedMap<String,String> matrixParameters = pathSegment.getMatrixParameters();
      return matrixParameters == null ? null : matrixParameters.get(((MatrixParam)annotation).value());
    }

    if (annotation.annotationType() == CookieParam.class) {
      final Map<String,Cookie> cookies = containerRequestContext.getCookies();
      if (cookies == null)
        return null;

      final String cookieParam = ((CookieParam)annotation).value();
      return cookies.get(cookieParam);
    }

    if (annotation.annotationType() == HeaderParam.class) {
      final String headerParam = ((HeaderParam)annotation).value();
      return containerRequestContext.getHeaderString(headerParam);
    }

    if (annotation.annotationType() == Context.class) {
      return annotationInjector.getContextObject(parameterType);
    }

    throw new UnsupportedOperationException("Unsupported param annotation type: " + annotation.annotationType());
  }

  boolean checkContentHeader(final String headerName, final Class<? extends Annotation> annotationClass, final ContainerRequestContext containerRequestContext) {
    final Annotation annotation = getResourceAnnotationProcessor(annotationClass).getAnnotation();
    if (annotation == null) {
      final String message = "@" + annotationClass.getSimpleName() + " annotation missing for " + resourceMethod.getDeclaringClass().getName() + "." + resourceMethod.getName() + "(" + ArrayUtil.toString(resourceMethod.getParameterTypes(), ',', Class::getName) + ")";
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

  Object service(final ResourceMatch resourceMatch, final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IOException, ServletException {
    checkAllowed(containerRequestContext);
    try {
      return resourceMethod.invoke(resourceMatch.getResourceInstance(annotationInjector), getParameters(containerRequestContext, annotationInjector, paramConverterProviders));
    }
    catch (final IllegalAccessException | InstantiationException e) {
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

  UriTemplate getUriTemplate() {
    return uriTemplate;
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
    final int c = uriTemplate.compareTo(o.uriTemplate);
    if (c != 0)
      return c;

    // FIXME: [JAX-RS 2.1 3.7.2 2.f]
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
    return Objects.equals(httpMethod, that.httpMethod) && securityAnnotation.equals(that.securityAnnotation) && resourceMethod.equals(that.resourceMethod) && resourceClass.equals(that.resourceClass) && uriTemplate.equals(that.uriTemplate) && consumesMatcher.equals(that.consumesMatcher) && producesMatcher.equals(that.producesMatcher);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    hashCode = 31 * hashCode + Objects.hashCode(httpMethod);
    hashCode = 31 * hashCode + securityAnnotation.hashCode();
    hashCode = 31 * hashCode + resourceMethod.hashCode();
    hashCode = 31 * hashCode + resourceClass.hashCode();
    hashCode = 31 * hashCode + uriTemplate.hashCode();
    hashCode = 31 * hashCode + consumesMatcher.hashCode();
    hashCode = 31 * hashCode + producesMatcher.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return (httpMethod != null ? httpMethod.value() : "*") + " " + uriTemplate;
  }
}