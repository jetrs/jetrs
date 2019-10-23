/* Copyright (c) 2019 JetRS
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;

import org.jetrs.common.Bootstrap;
import org.jetrs.common.EntityReaderProviderResource;
import org.jetrs.common.EntityWriterProviderResource;
import org.jetrs.common.ExceptionMappingProviderResource;
import org.jetrs.common.ProviderResource;
import org.jetrs.common.ReaderInterceptorEntityProviderResource;
import org.jetrs.common.WriterInterceptorEntityProviderResource;

class ServerBootstrap extends Bootstrap<ResourceManifest> {
  /**
   * http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html
   * Root resource classes are POJOs that are either annotated with @Path or have at least one
   * method annotated with @Path or a request method designator, such as @GET, @PUT, @POST, or
   * @DELETE. Resource methods are methods of a resource class annotated with a request method
   * designator. This section explains how to use JAX-RS to annotate Java classes to create
   * RESTful web services.
   */
  private static boolean isRootResource(final Class<?> cls) {
    if (Modifier.isAbstract(cls.getModifiers()) || Modifier.isInterface(cls.getModifiers()))
      return false;

    if (cls.isAnnotationPresent(Path.class))
      return true;

    try {
      final Method[] methods = cls.getMethods();
      for (final Method method : methods)
        if (!Modifier.isAbstract(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !Modifier.isNative(method.getModifiers()) && (method.isAnnotationPresent(Path.class) || method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(PUT.class) || method.isAnnotationPresent(DELETE.class) || method.isAnnotationPresent(HEAD.class)))
          return true;

      return false;
    }
    catch (final NoClassDefFoundError e) {
      return false;
    }
  }

  @Override
  protected <T>void addResourceOrProvider(final MultivaluedMap<String,ResourceManifest> resources, final List<ExceptionMappingProviderResource> exceptionMappers, final List<EntityReaderProviderResource> entityReaders, final List<EntityWriterProviderResource> entityWriters, final List<ProviderResource<ContainerRequestFilter>> requestFilters, final List<ProviderResource<ContainerResponseFilter>> responseFilters, final List<ReaderInterceptorEntityProviderResource> readerInterceptors, final List<WriterInterceptorEntityProviderResource> writerInterceptors, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders, final Class<? extends T> clazz, T singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (isRootResource(clazz)) {
      for (final Method method : clazz.getMethods()) {
        final Set<HttpMethod> httpMethodAnnotations = new HashSet<>(); // FIXME: Can this be done without a Collection?
        final Annotation[] annotations = method.getAnnotations();
        for (final Annotation annotation : annotations) {
          final HttpMethod httpMethodAnnotation = annotation.annotationType().getAnnotation(HttpMethod.class);
          if (httpMethodAnnotation != null)
            httpMethodAnnotations.add(httpMethodAnnotation);
        }

        for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations) {
          final ResourceManifest manifest = new ResourceManifest(httpMethodAnnotation, method, singleton);
          logger.info(httpMethodAnnotation.value() + " " + manifest.getPathPattern().getPattern().toString() + " -> " + clazz.getSimpleName() + "." + method.getName() + "()");
          resources.add(manifest.getHttpMethod().value().toUpperCase(), manifest);
        }
      }
    }
    else {
      super.addResourceOrProvider(resources, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, readerInterceptors, writerInterceptors, paramConverterProviders, singleton.getClass(), singleton);
    }
  }
}