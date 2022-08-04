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

package org.jetrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.libj.lang.PackageNotFoundException;

class ServerBootstrap extends Bootstrap<ResourceManifest> {
  private static final int defaultPriority = Priorities.USER;

  private static final Comparator<ProviderFactory<?>> priorityComparator = Comparator.nullsFirst((o1, o2) -> {
    final Priority p1 = o1.getProviderClass().getAnnotation(Priority.class);
    final Priority p2 = o2.getProviderClass().getAnnotation(Priority.class);
    final int v1 = p1 != null ? p1.value() : defaultPriority;
    final int v2 = p2 != null ? p2.value() : defaultPriority;
    return Integer.compare(v1, v2);
  });

  /**
   * http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html
   * Root resource classes are POJOs that are either annotated with {@code @Path} or have
   * at least one method annotated with @Path or a request method designator, such as
   * {@code @GET}, {@code @PUT}, {@code @POST}, or {@code @DELETE}. Resource methods are
   * methods of a resource class annotated with a request method designator. This section
   * explains how to use JAX-RS to annotate Java classes to create RESTful web services.
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

  private static <T>void add(final HttpMethod httpMethodAnnotation, final Method method, final String baseUri, final Path classPath, final Path methodPath, final List<? super ResourceManifest> resources, final Class<? extends T> clazz, final T singleton) {
    final ResourceManifest manifest = new ResourceManifest(httpMethodAnnotation, method, baseUri, classPath, methodPath, singleton);
    if (logger.isDebugEnabled())
      logger.debug((httpMethodAnnotation != null ? httpMethodAnnotation.value() : "*") + " " + manifest.getUriTemplate().toString() + " -> " + clazz.getSimpleName() + "." + method.getName() + "()");

    resources.add(manifest);
  }

  private final String baseUri;
  private final ParamConverterProviders.FactoryList paramConverterEntityProviderFactories;
  private final ContainerRequestFilterProviders.FactoryList preMatchContainerRequestFilterEntityProviderFactories;
  private final ContainerRequestFilterProviders.FactoryList containerRequestFilterEntityProviderFactories;
  private final ContainerResponseFilterProviders.FactoryList containerResponseFilterEntityProviderFactories;

  ServerBootstrap(final String baseUri,
    final ReaderInterceptorProviders.FactoryList readerInterceptorEntityProviderFactories,
    final WriterInterceptorProviders.FactoryList writerInterceptorEntityProviderFactories,
    final MessageBodyReaderProviders.FactoryList messageBodyReaderEntityProviderFactories,
    final MessageBodyWriterProviders.FactoryList messageBodyWriterEntityProviderFactories,
    final ExceptionMapperProviders.FactoryList exceptionMapperEntityProviderFactories,
    final ParamConverterProviders.FactoryList paramConverterEntityProviderFactories,
    final ContainerRequestFilterProviders.FactoryList preMatchContainerRequestFilterEntityProviderFactories,
    final ContainerRequestFilterProviders.FactoryList containerRequestFilterEntityProviderFactories,
    final ContainerResponseFilterProviders.FactoryList containerResponseFilterEntityProviderFactories
  ) {
    super(readerInterceptorEntityProviderFactories, writerInterceptorEntityProviderFactories, messageBodyReaderEntityProviderFactories, messageBodyWriterEntityProviderFactories, exceptionMapperEntityProviderFactories);
    this.baseUri = baseUri;
    this.paramConverterEntityProviderFactories = paramConverterEntityProviderFactories;
    this.preMatchContainerRequestFilterEntityProviderFactories = preMatchContainerRequestFilterEntityProviderFactories;
    this.containerRequestFilterEntityProviderFactories = containerRequestFilterEntityProviderFactories;
    this.containerResponseFilterEntityProviderFactories = containerResponseFilterEntityProviderFactories;
  }

  @Override
  <T>boolean addResourceOrProvider(final List<Consumer<Set<Class<?>>>> afterAdd, final List<ResourceManifest> resources, final Class<? extends T> clazz, final T singleton, final boolean scanned) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (clazz.isAnnotationPresent(Provider.class)) {
      if (ParamConverterProvider.class.isAssignableFrom(clazz))
        paramConverterEntityProviderFactories.superAdd(new ParamConverterProviders.Factory((Class<ParamConverterProvider>)clazz, (ParamConverterProvider)singleton));

      if (ContainerRequestFilter.class.isAssignableFrom(clazz))
        (clazz.isAnnotationPresent(PreMatching.class) ? preMatchContainerRequestFilterEntityProviderFactories : containerRequestFilterEntityProviderFactories).superAdd(new ContainerRequestFilterProviders.Factory((Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)singleton));

      if (ContainerResponseFilter.class.isAssignableFrom(clazz)) {
        containerResponseFilterEntityProviderFactories.superAdd(new ContainerResponseFilterProviders.Factory((Class<ContainerResponseFilter>)clazz, (ContainerResponseFilter)singleton));
        if (logger.isDebugEnabled() && clazz.isAnnotationPresent(PreMatching.class))
          logger.debug("@PreMatching annotation is not applicable to ContainerResponseFilter");
      }
    }

    if (!isRootResource(clazz))
      return super.addResourceOrProvider(afterAdd, resources, singleton == null ? clazz : singleton.getClass(), singleton, scanned);

    boolean added = false;
    final Method[] methods = clazz.getMethods();
    if (methods.length > 0) {
      final HashSet<HttpMethod> httpMethodAnnotations = new HashSet<>();
      for (final Method method : clazz.getMethods()) {
        final Annotation[] annotations = method.getAnnotations();
        for (final Annotation annotation : annotations) {
          final HttpMethod httpMethodAnnotation = annotation.annotationType().getAnnotation(HttpMethod.class);
          if (httpMethodAnnotation != null)
            httpMethodAnnotations.add(httpMethodAnnotation);
        }

        final Path classPath = clazz.getAnnotation(Path.class);
        final Path methodPath = method.getAnnotation(Path.class);
        if (httpMethodAnnotations.size() > 0) {
          for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations)
            add(httpMethodAnnotation, method, baseUri, classPath, methodPath, resources, clazz, singleton);

          httpMethodAnnotations.clear();
          added = true;
        }
        else if (classPath != null || methodPath != null) {
          // FIXME: This is for the case of "JAX-RS 2.1 3.4.1: Sub-Resource Locator"
          // FIXME: Need to use a Digraph or RefDigraph
          final Class<?> returnType = method.getReturnType();
          if (!returnType.isAnnotation() && !returnType.isAnonymousClass() && !returnType.isArray() && !returnType.isEnum() && !returnType.isInterface() && !returnType.isPrimitive() && !returnType.isSynthetic() && returnType != Void.class && acceptPackage(returnType.getPackage())) {
            afterAdd.add(resourceClasses -> {
              if (resourceClasses.contains(returnType))
                add(null, method, baseUri, classPath, methodPath, resources, clazz, singleton);
            });
          }
        }
      }
    }

    return added;
  }

  @Override
  void init(final Set<?> singletons, final Set<Class<?>> classes, final List<ResourceManifest> resources) throws IllegalAccessException, InstantiationException, InvocationTargetException, PackageNotFoundException, IOException {
    super.init(singletons, classes, resources);
    preMatchContainerRequestFilterEntityProviderFactories.superSort(priorityComparator);
    containerRequestFilterEntityProviderFactories.superSort(priorityComparator);
    containerResponseFilterEntityProviderFactories.superSort(priorityComparator);
  }
}