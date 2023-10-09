/* Copyright (c) 2023 JetRS
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.PackageLoader;
import org.libj.lang.PackageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ServerComponents extends Components {
  private static final Logger logger = LoggerFactory.getLogger(ServerComponents.class);

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

  private static <T> void register(final HttpMethod httpMethodAnnotation, final Method method, final String baseUri, final Path classPath, final Path methodPath, final ResourceInfos resourceInfos, final Class<? extends T> clazz, final T singleton) {
    final ResourceInfoImpl resourceInfo = new ResourceInfoImpl(resourceInfos, httpMethodAnnotation, method, baseUri, classPath, methodPath, singleton);
    if (logger.isDebugEnabled()) { logger.debug((httpMethodAnnotation != null ? httpMethodAnnotation.value() : "*") + " " + resourceInfo.getUriTemplate() + " -> " + clazz.getSimpleName() + "." + method.getName() + "()"); }
    resourceInfos.add(resourceInfo);
  }

  private final ResourceInfos resourceInfos;
  private final String baseUri;
  private ComponentSet<Component<ParamConverterProvider>> paramConverterComponents;
  private ComponentSet<Component<ContainerRequestFilter>> preMatchContainerRequestFilterComponents;
  private ComponentSet<Component<ContainerRequestFilter>> containerRequestFilterComponents;
  private ComponentSet<Component<ContainerResponseFilter>> containerResponseFilterComponents;
  private ArrayList<Runnable> afterAdds;

  ServerComponents(final Application application, final ResourceInfos resourceInfos, final String baseUri) throws IOException {
    super();
    this.resourceInfos = resourceInfos;
    this.baseUri = baseUri;

    final Set<Object> singletons = application.getSingletons();
    final Set<Class<?>> classes = application.getClasses();
    if (singletons == null && classes == null) {
      // Only scan the classpath if both singletons and classes are null
      final HashSet<Class<?>> initedClasses = new HashSet<>();
      final Predicate<Class<?>> initialize = (final Class<?> clazz) -> {
        if (!Modifier.isAbstract(clazz.getModifiers()) && !initedClasses.contains(clazz))
          register(clazz, null, false, null, -1);

        initedClasses.add(clazz);
        return false;
      };

      final PackageLoader contextPackageLoader = PackageLoader.getContextPackageLoader();
      for (final Package pkg : Package.getPackages()) { // [A]
        if (acceptPackage(pkg)) {
          try {
            contextPackageLoader.loadPackage(pkg, initialize);
          }
          catch (final IOException | PackageNotFoundException e) {
            if (logger.isDebugEnabled()) { logger.debug(e.getMessage(), e); }
          }
        }
      }
    }
    else {
      if (singletons != null && singletons.size() > 0)
        for (final Object singleton : singletons) // [S]
          if (singleton != null)
            register(singleton.getClass(), singleton, false, null, -1);

      if (classes != null && classes.size() > 0)
        for (final Class<?> clazz : classes) // [S]
          if (clazz != null)
            register(clazz, null, false, null, -1);
    }

    if (afterAdds != null)
      for (int i = 0, i$ = afterAdds.size(); i < i$; ++i) // [RA]
        afterAdds.get(i).run();

    if (resourceInfos != null) {
      resourceInfos.sort(null);
      for (int i = 0, i$ = resourceInfos.size(); i < i$; ++i) // [RA]
        resourceInfos.get(i).initDefaultValues(getParamConverterComponents());
    }
  }

  ComponentSet<Component<ParamConverterProvider>> getParamConverterComponents() {
    return paramConverterComponents != null ? paramConverterComponents : ComponentSet.EMPTY;
  }

  ComponentSet<Component<ContainerRequestFilter>> getPreMatchContainerRequestFilterComponents() {
    return preMatchContainerRequestFilterComponents != null ? preMatchContainerRequestFilterComponents : ComponentSet.EMPTY;
  }

  ComponentSet<Component<ContainerRequestFilter>> getContainerRequestFilterComponents() {
    return containerRequestFilterComponents != null ? containerRequestFilterComponents : ComponentSet.EMPTY;
  }

  ComponentSet<Component<ContainerResponseFilter>> getContainerResponseFilterComponents() {
    return containerResponseFilterComponents != null ? containerResponseFilterComponents : ComponentSet.EMPTY;
  }

  @Override
  int toCompositeSetArray(final ComponentSet<?>[] target, int index) {
    target[index++] = paramConverterComponents;
    target[index++] = preMatchContainerRequestFilterComponents;
    target[index++] = containerRequestFilterComponents;
    target[index++] = containerResponseFilterComponents;
    return super.toCompositeSetArray(target, index);
  }

  @Override
  int noComponentTypes() {
    return 9;
  }

  @Override
  boolean contains(final Class<?> componentClass) {
    if (ParamConverterProvider.class.isAssignableFrom(componentClass))
      return paramConverterComponents.containsComponent(componentClass);

    if (ContainerRequestFilter.class.isAssignableFrom(componentClass))
      return preMatchContainerRequestFilterComponents.containsComponent(componentClass) || containerRequestFilterComponents.containsComponent(componentClass);

    if (ContainerResponseFilter.class.isAssignableFrom(componentClass))
      return containerResponseFilterComponents.containsComponent(componentClass);

    return super.contains(componentClass);
  }

  @Override
  boolean contains(final Object component) {
    final Class<?> componentClass = component.getClass();
    if (ParamConverterProvider.class.isAssignableFrom(componentClass))
      return paramConverterComponents.containsComponent(component);

    if (ContainerRequestFilter.class.isAssignableFrom(componentClass))
      return preMatchContainerRequestFilterComponents.containsComponent(component) || containerRequestFilterComponents.containsComponent(component);

    if (ContainerResponseFilter.class.isAssignableFrom(componentClass))
      return containerResponseFilterComponents.containsComponent(component);

    return super.contains(component);
  }

  @Override
  Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    if (ParamConverterProvider.class.isAssignableFrom(componentClass))
      return paramConverterComponents.getContracts(componentClass);

    if (ContainerRequestFilter.class.isAssignableFrom(componentClass)) {
      final Map<Class<?>,Integer> contracts = preMatchContainerRequestFilterComponents.getContracts(componentClass);
      return contracts != null ? contracts : containerRequestFilterComponents.getContracts(componentClass);
    }

    if (ContainerResponseFilter.class.isAssignableFrom(componentClass))
      return containerResponseFilterComponents.getContracts(componentClass);

    return super.getContracts(componentClass);
  }

  @Override
  boolean isEmpty() {
    return super.isEmpty() && paramConverterComponents == null && preMatchContainerRequestFilterComponents == null && containerRequestFilterComponents == null && containerResponseFilterComponents == null;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T> boolean register(final Class<? extends T> clazz, final T instance, final boolean isDefaultProvider, final Map<Class<?>,Integer> contracts, final int priority) {
    boolean added = false;
    if (ParamConverterProvider.class.isAssignableFrom(clazz)) {
      if (paramConverterComponents == null)
        paramConverterComponents = new ComponentSet.Untyped<>();

      paramConverterComponents.register(new ParamConverterComponent((Class<ParamConverterProvider>)clazz, (ParamConverterProvider)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (ContainerRequestFilter.class.isAssignableFrom(clazz)) {
      final ComponentSet<Component<ContainerRequestFilter>> requestFilterComponents;
      if (AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class)) {
        if (preMatchContainerRequestFilterComponents == null)
          preMatchContainerRequestFilterComponents = new ComponentSet.Untyped<>();

        requestFilterComponents = preMatchContainerRequestFilterComponents;
      }
      else {
        if (containerRequestFilterComponents == null)
          containerRequestFilterComponents = new ComponentSet.Untyped<>();

        requestFilterComponents = containerRequestFilterComponents;
      }

      requestFilterComponents.register(new ContainerRequestFilterComponent((Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (ContainerResponseFilter.class.isAssignableFrom(clazz)) {
      if (containerResponseFilterComponents == null)
        containerResponseFilterComponents = new ComponentSet.Untyped<>();

      if (logger.isDebugEnabled() && AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class))
        logger.debug("@PreMatching annotation is not applicable to ContainerResponseFilter");

      containerResponseFilterComponents.register(new ContainerResponseFilterComponent((Class<ContainerResponseFilter>)clazz, (ContainerResponseFilter)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (!isRootResource(clazz))
      return super.register(instance == null ? clazz : instance.getClass(), instance, isDefaultProvider, contracts, priority) || added;

    final Method[] methods = clazz.getMethods();
    if (methods.length == 0)
      return added;

    final HashSet<HttpMethod> httpMethodAnnotations = new HashSet<>();
    for (final Method method : clazz.getMethods()) { // [A]
      final Path methodPath = AnnotationUtil.digestAnnotations(method, httpMethodAnnotations);
      final Path classPath = AnnotationUtil.getAnnotation(clazz, Path.class);
      if (httpMethodAnnotations.size() > 0) {
        for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations) // [S]
          register(httpMethodAnnotation, method, baseUri, classPath, methodPath, resourceInfos, clazz, instance);

        httpMethodAnnotations.clear();
        added = true;
      }
      else if (classPath != null || methodPath != null) {
        // FIXME: This is for the case of "JAX-RS 2.1 3.4.1: Sub-Resource Locator"
        // FIXME: Need to use a Digraph or RefDigraph
        final Class<?> returnType = method.getReturnType();
        if (!returnType.isAnnotation() && !returnType.isAnonymousClass() && !returnType.isArray() && !returnType.isEnum() && !returnType.isInterface() && !returnType.isPrimitive() && !returnType.isSynthetic() && returnType != Void.class && acceptPackage(returnType.getPackage())) {
          if (afterAdds == null)
            afterAdds = new ArrayList<>();

          afterAdds.add(() -> {
            for (final ResourceInfo resourceInfo : resourceInfos) {
              if (resourceInfo.getResourceClass() == returnType) {
                register(null, method, baseUri, classPath, methodPath, resourceInfos, clazz, instance);
                break;
              }
            }
          });
        }
      }
    }

    return added;
  }

  @Override
  protected ServerComponents clone() {
    final ServerComponents clone = (ServerComponents)super.clone();

    if (paramConverterComponents != null)
      clone.paramConverterComponents = paramConverterComponents.clone();

    if (preMatchContainerRequestFilterComponents != null)
      clone.preMatchContainerRequestFilterComponents = preMatchContainerRequestFilterComponents.clone();

    if (containerRequestFilterComponents != null)
      clone.containerRequestFilterComponents = containerRequestFilterComponents.clone();

    if (containerResponseFilterComponents != null)
      clone.containerResponseFilterComponents = containerResponseFilterComponents.clone();

    return clone;
  }
}