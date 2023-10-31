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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.Classes;
import org.libj.lang.IllegalAnnotationException;
import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResourceInfoImpl implements ResourceInfo, Comparable<ResourceInfoImpl> {
  private static final Logger logger = LoggerFactory.getLogger(ResourceInfoImpl.class);

  private static boolean logMissingHeaderWarning(final HttpHeader<?> httpHeader, final Class<?> type) {
    if (logger.isWarnEnabled()) { logger.warn("Unmatched @" + type.getSimpleName() + " for " + httpHeader.getName()); }
    return false;
  }

  private static Annotation findSecurityAnnotation(final Method method) {
    final Annotation annotation = findSecurityAnnotation(Classes.getAnnotations(method));
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
  private boolean consumesCalled;
  private boolean producesCalled;
  private Consumes consumes;
  private Produces produces;
  private boolean consumesMediaTypesCalled;
  private boolean producesMediaTypesCalled;
  private ServerMediaType[] consumesMediaTypes;
  private ServerMediaType[] producesMediaTypes;
  private DefaultValueImpl[] defaultValues;

  ResourceInfoImpl(final ResourceInfos resourceInfos, final HttpMethod httpMethod, final Method method, final String baseUri, final Path classPath, final Path methodPath, final Object singleton) {
    this.resourceInfos = resourceInfos;
    this.httpMethod = httpMethod;
    this.securityAnnotation = findSecurityAnnotation(method);
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
      if (fields.length > 0 && logger.isWarnEnabled())
        logger.warn("Fields with injectable annotations " + Arrays.toString(fields) + " will not be injected on singleton of class " + resourceClass.getName());
    }

    this.uriTemplate = new UriTemplate(baseUri, classPath, methodPath);
  }

  /**
   * Tests whether the method of the specified resourceInfo contains an entity parameter.
   *
   * @return {@code true} if the specified method contains an entity parameter; otherwise {@code false}.
   */
  private boolean hasEntityParameter() {
    OUT:
    for (final Annotation[] annotations : getMethodParameterAnnotations()) { // [A]
      for (final Annotation annotation : annotations) // [A]
        for (final Class<?> paramAnnotation : ContainerRequestContextImpl.injectableAnnotationTypes) // [A]
          if (paramAnnotation.equals(annotation.annotationType()))
            continue OUT;

      return true;
    }

    return false;
  }

  private <T extends Annotation> T getMethodClassAnnotation(final Class<T> annotationClass) {
    final T annotation = AnnotationUtil.getAnnotation(resourceMethod, annotationClass);
    return annotation != null ? annotation : AnnotationUtil.getAnnotation(resourceMethod.getDeclaringClass(), annotationClass);
  }

  Consumes getConsumes() {
    if (consumesCalled)
      return consumes;

    consumesCalled = true;
    return consumes = getMethodClassAnnotation(Consumes.class);
  }

  Produces getProduces() {
    if (producesCalled)
      return produces;

    producesCalled = true;
    return produces = getMethodClassAnnotation(Produces.class);
  }

  ServerMediaType[] getConsumesMediaTypes() {
    if (consumesMediaTypesCalled)
      return consumesMediaTypes;

    consumesMediaTypesCalled = true;
    final Consumes annotation = getConsumes();
    if (annotation != null && !hasEntityParameter())
      throw new IllegalAnnotationException(annotation, getResourceClass().getName() + "." + getMethodName() + "(" + ArrayUtil.toString(getMethodParameterTypes(), ',', Class::getName) + ") does not specify entity parameters, and thus cannot declare @Consumes annotation");

    return consumesMediaTypes = annotation != null ? ServerMediaType.valueOf(annotation.value()) : MediaTypes.WILDCARD_SERVER_TYPE;
  }

  ServerMediaType[] getProducesMediaTypes() {
    if (producesMediaTypesCalled)
      return producesMediaTypes;

    producesMediaTypesCalled = true;
    final Produces annotation = getProduces();
    if (annotation != null && Void.TYPE.equals(getMethodReturnType()))
      throw new IllegalAnnotationException(annotation, getResourceClass().getName() + "." + getMethodName() + "(" + ArrayUtil.toString(getMethodParameterTypes(), ',', Class::getName) + ") is void return type, and thus cannot declare @Produces annotation");

    return producesMediaTypes = annotation != null ? ServerMediaType.valueOf(annotation.value()) : MediaTypes.WILDCARD_SERVER_TYPE;
  }

  void initDefaultValues(final ComponentSet<Component<ParamConverterProvider>> paramConverterComponents) throws IOException {
    if (defaultValues != null)
      throw new IllegalStateException();

    resourceInfos.initDefaultValues(resourceClass, paramConverterComponents);
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
          defaultValues[i] = ResourceInfos.digestDefaultValue((DefaultValue)parameterAnnotation, methodParameterTypes[i], methodGenericParameterTypes[i], parameterAnnotations, paramConverterComponents);
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
    return MediaTypes.getCompatible(getConsumesMediaTypes(), contentType, null).length != 0;
  }

  MediaType[] getCompatibleAccept(final List<MediaType> acceptMediaTypes, final List<String> acceptCharsets) {
    return MediaTypes.getCompatible(getProducesMediaTypes(), acceptMediaTypes, acceptCharsets);
  }

  @SuppressWarnings("unchecked")
  boolean checkContentHeader(final HttpHeader<MediaType> httpHeader, final HttpHeadersImpl httpHeaders) {
    final List<?> headerValue = httpHeaders.getMirrorMap().get(httpHeader.getName());
    if (headerValue == null)
      return logMissingHeaderWarning(httpHeader, Consumes.class);

    return MediaTypes.getCompatible(getConsumesMediaTypes(), (List<MediaType>)headerValue, null) != null || logMissingHeaderWarning(httpHeader, Consumes.class);
  }

  private static void checkAuthorized(final Annotation securityAnnotation, final ContainerRequestContext requestContext) {
    if (securityAnnotation instanceof PermitAll)
      return;

    if (securityAnnotation instanceof DenyAll)
      throw new ForbiddenException();

    final RolesAllowed rolesAllowed = (RolesAllowed)securityAnnotation;
    final SecurityContext securityContext = requestContext.getSecurityContext();
    if (securityContext == null) {
      final StringBuilder b = new StringBuilder();

      // FIXME: What about "Proxy-Authenticate"?
      final String[] roles = rolesAllowed.value();
      b.append("realm=\"").append(roles[0]).append('"');
      final String challenge = b.toString();
      if (roles.length == 1)
        throw new NotAuthorizedException(challenge);

      final Object[] challenges = new Object[roles.length - 1];
      for (int i = 1, i$ = roles.length; i < i$; ++i) { // [A]
        b.setLength(0);
        b.append("realm=\"").append(roles[i]).append('"');
        challenges[i - 1] = b.toString();
      }

      throw new NotAuthorizedException(challenge, challenges);
    }

    if (securityContext.getUserPrincipal() != null)
      for (final String role : rolesAllowed.value()) // [A]
        if (securityContext.isUserInRole(role))
          return;

    throw new ForbiddenException();
  }

  Object service(final ResourceMatch resourceMatch, final ContainerRequestContextImpl requestContext) throws IOException, ServletException {
    if (securityAnnotation != null)
      checkAuthorized(securityAnnotation, requestContext);

    try {
      return requestContext.invokeMethod(resourceMatch.getResourceInstance(requestContext));
    }
    catch (final IllegalAccessException | InstantiationException e) {
      throw new ServletException(e);
    }
    catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      if (cause instanceof IOException)
        throw (IOException)cause;

      if (cause instanceof ServletException)
        throw (ServletException)cause;

      throw new ServletException(cause);
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
    return Objects.equals(httpMethod, that.httpMethod) && Objects.equals(securityAnnotation, that.securityAnnotation) && resourceMethod.equals(that.resourceMethod) && resourceClass.equals(that.resourceClass) && uriTemplate.equals(that.uriTemplate);
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