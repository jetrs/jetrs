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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

class ServerBootstrap extends Bootstrap<ResourceInfos> {
  private static final int defaultPriority = Priorities.USER;

  private static final Comparator<Component<?>> priorityComparator = Comparator.nullsFirst((o1, o2) -> {
    final Priority p1 = o1.getProviderClass().getAnnotation(Priority.class);
    final Priority p2 = o2.getProviderClass().getAnnotation(Priority.class);
    final int v1 = p1 != null ? p1.value() : defaultPriority;
    final int v2 = p2 != null ? p2.value() : defaultPriority;
    return Integer.compare(v1, v2);
  });

  /**
   * http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html Root resource classes are POJOs that are either annotated with
   * {@code @Path} or have at least one method annotated with @Path or a request method designator, such as {@code @GET},
   * {@code @PUT}, {@code @POST}, or {@code @DELETE}. Resource methods are methods of a resource class annotated with a request method
   * designator. This section explains how to use JAX-RS to annotate Java classes to create RESTful web services.
   */
  private static boolean isRootResource(final Class<?> clazz) {
    if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))
      return false;

    if (AnnotationUtil.isAnnotationPresent(clazz, Path.class))
      return true;

    try {
      for (final Method method : clazz.getMethods()) // [A]
        if (!Modifier.isAbstract(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !Modifier.isNative(method.getModifiers()) && AnnotationUtil.isAnyAnnotationPresent(method, Path.class, GET.class, POST.class, PUT.class, DELETE.class, HEAD.class))
          return true;

      return false;
    }
    catch (final NoClassDefFoundError e) {
      return false;
    }
  }

  private static <T> void add(final HttpMethod httpMethodAnnotation, final Method method, final String baseUri, final Path classPath, final Path methodPath, final ResourceInfos resourceInfos, final Class<? extends T> clazz, final T singleton) {
    final ResourceInfoImpl resourceInfo = new ResourceInfoImpl(resourceInfos, httpMethodAnnotation, method, baseUri, classPath, methodPath, singleton);
    if (logger.isDebugEnabled()) { logger.debug((httpMethodAnnotation != null ? httpMethodAnnotation.value() : "*") + " " + resourceInfo.getUriTemplate() + " -> " + clazz.getSimpleName() + "." + method.getName() + "()"); }
    resourceInfos.add(resourceInfo);
  }

  private final String baseUri;
  private final ArrayList<Component<ParamConverterProvider>> paramConverterComponents;
  private final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterComponents;
  private final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterComponents;
  private final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterComponents;

  ServerBootstrap(
    final String baseUri,
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents,
    final ArrayList<Component<ParamConverterProvider>> paramConverterComponents,
    final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterComponents,
    final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterComponents,
    final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterComponents
  ) {
    super(readerInterceptorComponents, writerInterceptorComponents, messageBodyReaderComponents, messageBodyWriterComponents, exceptionMapperComponents);
    this.baseUri = baseUri;
    this.paramConverterComponents = paramConverterComponents;
    this.preMatchContainerRequestFilterComponents = preMatchContainerRequestFilterComponents;
    this.containerRequestFilterComponents = containerRequestFilterComponents;
    this.containerResponseFilterComponents = containerResponseFilterComponents;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T> boolean addResourceOrProvider(final ArrayList<Consumer<Set<Class<?>>>> afterAdd, final ResourceInfos resourceInfos, final Class<? extends T> clazz, final T singleton, final boolean scanned) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (ParamConverterProvider.class.isAssignableFrom(clazz))
      paramConverterComponents.add(new ParamConverterComponent((Class<ParamConverterProvider>)clazz, (ParamConverterProvider)singleton));

    if (ContainerRequestFilter.class.isAssignableFrom(clazz))
      (AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class) ? preMatchContainerRequestFilterComponents : containerRequestFilterComponents).add(new ContainerRequestFilterComponent((Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)singleton));

    if (ContainerResponseFilter.class.isAssignableFrom(clazz)) {
      containerResponseFilterComponents.add(new ContainerResponseFilterComponent((Class<ContainerResponseFilter>)clazz, (ContainerResponseFilter)singleton));
      if (logger.isDebugEnabled() && AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class))
        logger.debug("@PreMatching annotation is not applicable to ContainerResponseFilter");
    }

    if (!isRootResource(clazz))
      return super.addResourceOrProvider(afterAdd, resourceInfos, singleton == null ? clazz : singleton.getClass(), singleton, scanned);

    boolean added = false;
    final Method[] methods = clazz.getMethods();
    if (methods.length > 0) {
      final HashSet<HttpMethod> httpMethodAnnotations = new HashSet<>();
      for (final Method method : clazz.getMethods()) { // [A]
        final Path methodPath = AnnotationUtil.digestAnnotations(method, httpMethodAnnotations);
        final Path classPath = AnnotationUtil.getAnnotation(clazz, Path.class);
        if (httpMethodAnnotations.size() > 0) {
          for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations) // [S]
            add(httpMethodAnnotation, method, baseUri, classPath, methodPath, resourceInfos, clazz, singleton);

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
                add(null, method, baseUri, classPath, methodPath, resourceInfos, clazz, singleton);
            });
          }
        }
      }
    }

    return added;
  }

  @Override
  void init(final Set<Object> singletons, final Set<Class<?>> classes, final ResourceInfos resourceInfos) throws IllegalAccessException, InstantiationException, InvocationTargetException, IOException {
    super.init(singletons, classes, resourceInfos);
    preMatchContainerRequestFilterComponents.sort(priorityComparator);
    containerRequestFilterComponents.sort(priorityComparator);
    containerResponseFilterComponents.sort(priorityComparator);
    paramConverterComponents.sort(priorityComparator);
    for (int i = 0, i$ = resourceInfos.size(); i < i$; ++i) // [RA]
      resourceInfos.get(i).initDefaultValues(paramConverterComponents);
  }
}