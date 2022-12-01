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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResourceInfoImpl implements ResourceInfo, Comparable<ResourceInfoImpl> {
  private static final Logger logger = LoggerFactory.getLogger(ResourceInfoImpl.class);
  private static final PermitAll permitAll = new PermitAll() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return getClass();
    }
  };

  private static boolean logMissingHeaderWarning(final HttpHeader<?> httpHeader, final Class<?> type) {
    logger.warn("Unmatched @" + type.getSimpleName() + " for " + httpHeader.getName());
    return false;
  }

  private static Annotation findSecurityAnnotation(final Method method) {
    final Annotation annotation = findSecurityAnnotation(method.getAnnotations());
    return annotation != null ? annotation : findSecurityAnnotation(method.getDeclaringClass().getAnnotations());
  }

  private static Annotation findSecurityAnnotation(final Annotation ... annotations) {
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType() == PermitAll.class || annotation.annotationType() == DenyAll.class || annotation.annotationType() == RolesAllowed.class)
        return annotation;

    return null;
  }

  private final ResourceInfos resourceInfos;
  private final HttpMethod httpMethod;
  private final Annotation securityAnnotation;
  private final Method resourceMethod;
  private final String methodName;
  private final Annotation[] methodAnnotations;
  private final Annotation[][] methodParameterAnnotations;
  private final int methodParameterCount;
  private final Parameter[] methodParameters;
  private final Class<?>[] methodParameterTypes;
  private final Type[] methodGenericParameterTypes;
  private final Class<?> methodReturnClass;
  private final Type methodReturnType;
  private final Class<?> resourceClass;
  private final Object singleton;
  private final UriTemplate uriTemplate;
  private MediaTypeAnnotationProcessor<Consumes> consumesMatcher;
  private MediaTypeAnnotationProcessor<Produces> producesMatcher;

  ResourceInfoImpl(final ResourceInfos resourceInfos, final HttpMethod httpMethod, final Method method, final String baseUri, final Path classPath, final Path methodPath, final Object singleton) {
    this.resourceInfos = resourceInfos;
    this.httpMethod = httpMethod;
    final Annotation securityAnnotation = findSecurityAnnotation(method);
    this.securityAnnotation = securityAnnotation != null ? securityAnnotation : permitAll;
    this.resourceMethod = method;
    this.methodName = method.getName();
    this.methodAnnotations = AnnotationUtil.getAnnotations(method);
    this.methodParameterAnnotations = method.getParameterAnnotations();
    this.methodParameterCount = method.getParameterCount();
    this.methodParameters = method.getParameters();
    this.methodParameterTypes = method.getParameterTypes();
    this.methodGenericParameterTypes = method.getGenericParameterTypes();
    this.methodReturnClass = method.getReturnType();
    this.methodReturnType = method.getGenericReturnType();
    this.resourceClass = method.getDeclaringClass();
    this.singleton = singleton;
    if (singleton != null) {
      final Field[] fields = ContainerRequestContextImpl.getContextFields(singleton.getClass());
      if (fields.length > 0)
        logger.warn("Fields with injectable annotations " + Arrays.toString(fields) + " will not be injected on singleton of class " + resourceClass.getName());
    }

    this.uriTemplate = new UriTemplate(baseUri, classPath, methodPath);
  }

  private MediaTypeAnnotationProcessor<Consumes> getConsumesMatcher() {
    return consumesMatcher == null ? consumesMatcher = new MediaTypeAnnotationProcessor<>(this, Consumes.class) : consumesMatcher;
  }

  private MediaTypeAnnotationProcessor<Produces> getProducesMatcher() {
    return producesMatcher == null ? producesMatcher = new MediaTypeAnnotationProcessor<>(this, Produces.class) : producesMatcher;
  }

  private DefaultValueImpl[] defaultValues;

  void initDefaultValues(final ArrayList<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories) {
    if (defaultValues != null)
      throw new IllegalStateException();

    resourceInfos.initDefaultValues(resourceClass, paramConverterProviderFactories);
    final int length = methodParameters.length;
    if (length == 0) {
      defaultValues = DefaultValueImpl.EMPTY_ARRAY;
      return;
    }

    defaultValues = new DefaultValueImpl[length];
    for (int i = 0; i < length; ++i) { // [A]
      final Annotation[] parameterAnnotations = methodParameterAnnotations[i];
      for (final Annotation parameterAnnotation : parameterAnnotations) { // [A]
        if (parameterAnnotation instanceof DefaultValue) {
          defaultValues[i] = ResourceInfos.digestDefaultValue((DefaultValue)parameterAnnotation, methodParameterTypes[i], methodGenericParameterTypes[i], parameterAnnotations, paramConverterProviderFactories);
          break;
        }
      }
    }
  }

  DefaultValueImpl getDefaultValue(final AnnotatedElement element, final int parameterIndex) {
    if (parameterIndex != -1)
      return defaultValues[parameterIndex];

    final HashMap<AnnotatedElement,DefaultValueImpl> defaultValues = resourceInfos.getDefaultValues(resourceClass);
    return defaultValues == null ? null : defaultValues.get(element);
  }

  String getMethodName() {
    return methodName;
  }

  public int getParameterCount() {
    return methodParameterCount;
  }

  Parameter[] getMethodParameters() {
    return methodParameters;
  }

  Class<?>[] getMethodParameterTypes() {
    return methodParameterTypes;
  }

  Type[] getMethodGenericParameterTypes() {
    return methodGenericParameterTypes;
  }

  Annotation[][] getMethodParameterAnnotations() {
    return methodParameterAnnotations;
  }

  Annotation[] getMethodAnnotations() {
    return methodAnnotations;
  }

  Class<?> getMethodReturnClass() {
    return methodReturnClass;
  }

  Type getMethodReturnType() {
    return methodReturnType;
  }

  @Override
  public Method getResourceMethod() {
    return resourceMethod;
  }

  @Override
  public Class<?> getResourceClass() {
    return resourceClass;
  }

  Object getSingleton() {
    return singleton;
  }

  boolean isCompatibleContentType(final MediaType contentType) {
    return getConsumesMatcher().getCompatibleMediaType(contentType, null) != null;
  }

  CompatibleMediaType[] getCompatibleAccept(final List<MediaType> acceptMediaTypes, final List<String> acceptCharsets) {
    return getProducesMatcher().getCompatibleMediaType(acceptMediaTypes, acceptCharsets);
  }

  @SuppressWarnings("unchecked")
  boolean checkContentHeader(final HttpHeader<MediaType> httpHeader, final Class<? extends Annotation> annotationClass, final ContainerRequestContextImpl containerRequestContext) {
    final Annotation annotation = getResourceAnnotationProcessor(annotationClass).getAnnotation();
    if (annotation == null) {
      final String message = "@" + annotationClass.getSimpleName() + " annotation missing for " + resourceMethod.getDeclaringClass().getName() + "." + resourceMethod.getName() + "(" + ArrayUtil.toString(resourceMethod.getParameterTypes(), ',', Class::getName) + ")";
      if (annotationClass == Consumes.class)
        throw new IllegalStateException(message);

      logger.warn(message);
      return true;
    }

    final List<?> headerValue = containerRequestContext.getHttpHeaders().getMirrorMap().get(httpHeader.getName());
    if (headerValue == null)
      return logMissingHeaderWarning(httpHeader, annotationClass);

    final String[] annotationValue = annotationClass == Produces.class ? ((Produces)annotation).value() : annotationClass == Consumes.class ? ((Consumes)annotation).value() : null;
    final ServerMediaType[] required = ServerMediaType.valueOf(annotationValue);
    return MediaTypes.getCompatible(required, (List<MediaType>)headerValue, null) != null || logMissingHeaderWarning(httpHeader, annotationClass);
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
      for (final String role : rolesAllowed.value()) // [A]
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
    for (int i = 1, i$ = roles.length; i < i$; ++i) { // [A]
      builder.setLength(resetLen);
      builder.append("realm=\"").append(roles[i]).append('"');
      challenges[i - 1] = builder.toString();
    }

    throw new NotAuthorizedException(challenge, challenges);
  }

  Object service(final ResourceMatch resourceMatch, final ContainerRequestContextImpl requestContext) throws IOException, ServletException {
    checkAllowed(requestContext);
    try {
      final Object instance = resourceMatch.getResourceInstance(requestContext);
      return requestContext.invokeMethod(instance);
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
    return annotationClass == Consumes.class ? (MediaTypeAnnotationProcessor<T>)getConsumesMatcher() : annotationClass == Produces.class ? (MediaTypeAnnotationProcessor<T>)getProducesMatcher() : null;
  }

  boolean isRestricted() {
    return securityAnnotation instanceof DenyAll || securityAnnotation instanceof RolesAllowed;
  }

  @Override
  public int compareTo(final ResourceInfoImpl o) {
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

    if (!(obj instanceof ResourceInfoImpl))
      return false;

    final ResourceInfoImpl that = (ResourceInfoImpl)obj;
    return Objects.equals(httpMethod, that.httpMethod) && securityAnnotation.equals(that.securityAnnotation) && resourceMethod.equals(that.resourceMethod) && resourceClass.equals(that.resourceClass) && uriTemplate.equals(that.uriTemplate);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    hashCode = 31 * hashCode + Objects.hashCode(httpMethod);
    hashCode = 31 * hashCode + securityAnnotation.hashCode();
    hashCode = 31 * hashCode + resourceMethod.hashCode();
    hashCode = 31 * hashCode + resourceClass.hashCode();
    hashCode = 31 * hashCode + uriTemplate.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    return (httpMethod != null ? httpMethod.value() : "*") + " " + uriTemplate;
  }
}