/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.safris.commons.lang.Arrays;
import org.safris.commons.lang.Strings;
import org.safris.xrs.server.core.ContextInjector;
import org.safris.xrs.server.util.MediaTypes;
import org.safris.xrs.server.util.ParameterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManifest {
  private static final Logger logger = LoggerFactory.getLogger(ResourceManifest.class);

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
  private final Class<?> serviceClass;
  private final PathPattern pathPattern;
  private final MediaTypeMatcher<Consumes> consumesMatcher;
  private final MediaTypeMatcher<Produces> producesMatcher;

  public ResourceManifest(final HttpMethod httpMethod, final Method method) {
    this.httpMethod = httpMethod;
    final Annotation securityAnnotation = findSecurityAnnotation(method);
    this.securityAnnotation = securityAnnotation != null ? securityAnnotation : new PermitAll() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return getClass();
      }
    };
    this.method = method;
    this.serviceClass = method.getDeclaringClass();
    this.pathPattern = new PathPattern(method);
    this.consumesMatcher = new MediaTypeMatcher<Consumes>(method, Consumes.class);
    this.producesMatcher = new MediaTypeMatcher<Produces>(method, Produces.class);
  }

  public boolean matches(final RequestMatchParams matchParams) {
    if (!httpMethod.value().toUpperCase().equals(matchParams.getMethod()))
      return false;

    final String path = matchParams.getPath();
    if (!pathPattern.matches(path))
      return false;

    final Set<MediaType> accept = matchParams.getAccept();
    if (!producesMatcher.matches(accept))
      return false;

    final Set<MediaType> contentType = matchParams.getContentType();
    if (!consumesMatcher.matches(contentType))
      return false;

    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object[] getParameters(final Method method, final ContainerRequestContext containerRequestContext, final ContextInjector injectionContext, final List<ParamConverterProvider> paramConverterProviders) throws IOException {
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final Type[] genericParameterTypes = method.getGenericParameterTypes();
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    if (parameterTypes.length == 0)
      return null;

    final Object[] parameters = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      final Class<?> parameterType = parameterTypes[i];
      final Type genericParameterType = genericParameterTypes[i];
      final Annotation[] annotations = parameterAnnotations[i];
      Annotation paramAnnotation = null;
      for (final Annotation annotation : annotations) {
        if (annotation.annotationType() == QueryParam.class || annotation.annotationType() == PathParam.class || annotation.annotationType() == MatrixParam.class || annotation.annotationType() == CookieParam.class || annotation.annotationType() == HeaderParam.class || annotation.annotationType() == Context.class) {
          if (paramAnnotation == null)
            paramAnnotation = annotation;
          else
            throw new WebApplicationException("Conflicting annotations found: " + paramAnnotation.annotationType().getName() + " and " + annotation.annotationType().getName());
        }
      }

      if (paramAnnotation == null) {
        final Providers providers = injectionContext.getInjectableObject(Providers.class);
        final MessageBodyReader messageBodyReader = providers.getMessageBodyReader(parameterType, genericParameterType, annotations, containerRequestContext.getMediaType());
        if (messageBodyReader != null)
          parameters[i] = messageBodyReader.readFrom(parameterType, parameterType.getGenericSuperclass(), parameterType.getAnnotations(), containerRequestContext.getMediaType(), containerRequestContext.getHeaders(), containerRequestContext.getEntityStream());
        else
          throw new WebApplicationException("Could not find MessageBodyReader for type: " + parameterType.getClass().getName());
      }
      else {
        try {
          if (paramAnnotation.annotationType() == QueryParam.class) {
            final boolean decode = ParameterUtil.decode(annotations);
            parameters[i] = ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, containerRequestContext.getUriInfo().getQueryParameters(decode).get(((QueryParam)paramAnnotation).value()), paramConverterProviders);
          }
          else if (paramAnnotation.annotationType() == PathParam.class) {
            final boolean decode = ParameterUtil.decode(annotations);
            final String pathParam = ((PathParam)paramAnnotation).value();
            parameters[i] = ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, containerRequestContext.getUriInfo().getPathParameters(decode).get(pathParam), paramConverterProviders);
          }
          else if (paramAnnotation.annotationType() == MatrixParam.class) {
            // TODO:
            throw new UnsupportedOperationException();
          }
          else if (paramAnnotation.annotationType() == CookieParam.class) {
            // TODO:
            throw new UnsupportedOperationException();
          }
          else if (paramAnnotation.annotationType() == HeaderParam.class) {
            // TODO:
            throw new UnsupportedOperationException();
          }
          else if (paramAnnotation.annotationType() == Context.class) {
            parameters[i] = injectionContext.getInjectableObject(parameterType);
          }
        }
        catch (final ReflectiveOperationException e) {
          if (paramAnnotation.annotationType() == MatrixParam.class || paramAnnotation.annotationType() == QueryParam.class || paramAnnotation.annotationType() == PathParam.class)
            throw new NotFoundException(e);

          throw new BadRequestException(e);
        }
      }
    }

    return parameters;
  }

  protected boolean checkHeader(final String headerName, final Class<? extends Annotation> annotationClass, final ContainerRequestContext containerRequestContext) {
    final Annotation annotation = getMatcher(annotationClass).getAnnotation();
    if (annotation == null) {
      final String message = "@" + annotationClass.getSimpleName() + " annotation missing for " + method.getDeclaringClass().getName() + "." + Strings.toTitleCase(containerRequestContext.getMethod().toLowerCase()) + "()";
      if (annotationClass == Consumes.class)
        throw new RuntimeException(message);

      logger.warn(message);
      return true;
    }

    final String headerValue = containerRequestContext.getHeaderString(headerName);
    if (headerValue == null || headerValue.length() == 0)
      return logMissingHeaderWarning(headerName, annotationClass);

    final String[] headerValueParts = headerValue.split(",");
    final MediaType[] test = MediaTypes.parse(headerValueParts);

    final String[] annotationValue = annotationClass == Produces.class ? ((Produces)annotation).value() : annotationClass == Consumes.class ? ((Consumes)annotation).value() : null;
    // FIXME: Order matters, and also the q value
    final MediaType[] required = MediaTypes.parse(annotationValue);
    if (MediaTypes.matches(required, test))
      return true;

    return logMissingHeaderWarning(headerName, annotationClass);
  }

  private static void allow(final Annotation securityAnnotation, final ContainerRequestContext containerRequestContext) {
    if (securityAnnotation instanceof PermitAll)
      return;

    if (securityAnnotation instanceof DenyAll)
      throw new ForbiddenException("@DenyAll");

    if (containerRequestContext.getSecurityContext().getUserPrincipal() == null)
      throw new NotAuthorizedException("Unauthorized");

    if (securityAnnotation instanceof RolesAllowed)
      for (final String role : ((RolesAllowed)securityAnnotation).value())
        if (containerRequestContext.getSecurityContext().isUserInRole(role))
          return;

    throw new ForbiddenException("@RolesAllowed(" + Arrays.toString(((RolesAllowed)securityAnnotation).value(), ",") + ")");
  }

  public Object service(final ContainerRequestContext containerRequestContext, final ContextInjector injectionContext, final List<ParamConverterProvider> paramConverterProviders) throws ServletException, IOException {
    allow(securityAnnotation, containerRequestContext);

    try {
      final Object[] parameters = getParameters(method, containerRequestContext, injectionContext, paramConverterProviders);

      final Object object = serviceClass.newInstance();
      return parameters != null ? method.invoke(object, parameters) : method.invoke(object);
    }
    catch (final IllegalAccessException | InstantiationException e) {
      throw new WebApplicationException(e);
    }
    catch (final InvocationTargetException e) {
      // FIXME: Hmm, this is an interesting idea to help reduce the noise in Exceptions from dynamically invoked methods
      if (e.getCause() instanceof WebApplicationException)
        throw (WebApplicationException)e.getCause();

      if (e.getCause() instanceof ServletException)
        throw (ServletException)e.getCause();

      if (e.getCause() instanceof IOException)
        throw (IOException)e.getCause();

      throw new WebApplicationException(e);
    }
    catch (final IllegalArgumentException | IOException e) {
      throw new WebApplicationException(e);
    }
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public PathPattern getPathPattern() {
    return pathPattern;
  }

  @SuppressWarnings("unchecked")
  public <T extends Annotation>MediaTypeMatcher<T> getMatcher(final Class<T> annotationClass) {
    return annotationClass == Consumes.class ? (MediaTypeMatcher<T>)consumesMatcher : annotationClass == Produces.class ? (MediaTypeMatcher<T>)producesMatcher : null;
  }

  public boolean isRestricted() {
    return securityAnnotation instanceof DenyAll || securityAnnotation instanceof RolesAllowed;
  }
}