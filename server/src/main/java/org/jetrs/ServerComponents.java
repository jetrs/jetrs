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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
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
import org.libj.util.function.ThrowingPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ServerComponents extends Components {
  private static final Logger logger = LoggerFactory.getLogger(ServerComponents.class);
  @SuppressWarnings("unchecked")
  private static final Class<Annotation>[] rootResourceAnnotations = new Class[] {Path.class, GET.class, POST.class, PUT.class, DELETE.class, OPTIONS.class, HEAD.class, PATCH.class};
  private static final String[] excludeStartsWith = {"jdk.", "java.", "javax.", "com.sun.", "sun.", "org.w3c.", "org.xml.", "org.jvnet.", "org.joda.", "org.jcp.", "apple.security."};

  // FIXME: Can use a RadixTree here
  private static boolean acceptPackage(final Package pkg) {
    final String name = pkg.getName();
    for (int i = 0, i$ = excludeStartsWith.length; i < i$; ++i) // [A]
      if (name.startsWith(excludeStartsWith[i]))
        return false;

    return true;
  }

  /**
   * http://docs.oracle.com/javaee/6/tutorial/doc/gilik.html Root resource classes are POJOs that are either annotated with
   * {@code @Path} or have at least one method annotated with @Path or a request method designator, such as {@code @GET},
   * {@code @PUT}, {@code @POST}, or {@code @DELETE}. Resource methods are methods of a resource class annotated with a request method
   * designator. This section explains how to use JAX-RS to annotate Java classes to create RESTful web services.
   */
  private static boolean isRootResource(final Class<?> clazz) {
    final int modifiers = clazz.getModifiers();
    if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))
      return false;

    if (AnnotationUtil.isAnnotationPresent(clazz, Path.class))
      return true;

    try {
      for (final Method method : clazz.getMethods()) // [A]
        if (!Modifier.isAbstract(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !Modifier.isNative(method.getModifiers()) && AnnotationUtil.isAnnotationPresent(method, rootResourceAnnotations))
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
  private ArrayList<Runnable> afterRegister;

  ServerComponents(final Application application, final ResourceInfos resourceInfos, final String baseUri) throws IOException {
    this.resourceInfos = resourceInfos;
    this.baseUri = baseUri;

    register(application.getClasses(), application.getSingletons());

    final Set<Class<?>> classes = application.getClasses();
    final Set<Object> singletons = application.getSingletons();
    if (singletons == null && classes == null) {
      // Only scan the classpath if both singletons and classes are null
      final HashSet<Class<?>> initedClasses = new HashSet<>();
      final ThrowingPredicate<Class<?>,ReflectiveOperationException> initialize = (final Class<?> clazz) -> {
        if (!Modifier.isAbstract(clazz.getModifiers()) && initedClasses.add(clazz))
          register(clazz, null, false, null, -1);

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

    registerDefaultProviders();

    if (afterRegister != null) {
      for (int i = 0, i$ = afterRegister.size(); i < i$; ++i) // [RA]
        afterRegister.get(i).run();

      afterRegister = null;
    }

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
    return paramConverterComponents == null && preMatchContainerRequestFilterComponents == null && containerRequestFilterComponents == null && containerResponseFilterComponents == null && super.isEmpty();
  }

  @Override
  @SuppressWarnings("unchecked")
  <T> boolean register(final Class<? extends T> clazz, final T singleton, final boolean isDefaultProvider, final Map<Class<?>,Integer> contracts, final int priority) {
    boolean changed = false;
    if (ParamConverterProvider.class.isAssignableFrom(clazz)) {
      paramConverterComponents = ParamConverterComponent.register(paramConverterComponents, (Class<ParamConverterProvider>)clazz, (ParamConverterProvider)singleton, isDefaultProvider, contracts, priority);
      changed = true;
    }

    if (ContainerRequestFilter.class.isAssignableFrom(clazz)) {
      if (AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class))
        preMatchContainerRequestFilterComponents = ContainerRequestFilterComponent.register(preMatchContainerRequestFilterComponents, (Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)singleton, isDefaultProvider, contracts, priority);
      else
        containerRequestFilterComponents = ContainerRequestFilterComponent.register(containerRequestFilterComponents, (Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)singleton, isDefaultProvider, contracts, priority);

      changed = true;
    }

    if (ContainerResponseFilter.class.isAssignableFrom(clazz)) {
      if (logger.isDebugEnabled() && AnnotationUtil.isAnnotationPresent(clazz, PreMatching.class))
        logger.debug("@PreMatching annotation is not applicable to ContainerResponseFilter");

      containerResponseFilterComponents = ContainerResponseFilterComponent.register(containerResponseFilterComponents, (Class<ContainerResponseFilter>)clazz, (ContainerResponseFilter)singleton, isDefaultProvider, contracts, priority);
      changed = true;
    }

    if (!isRootResource(clazz))
      return super.register(clazz, singleton, isDefaultProvider, contracts, priority) || changed;

    final Method[] methods = clazz.getMethods();
    if (methods.length == 0)
      return changed;

    final HashSet<HttpMethod> httpMethodAnnotations = new HashSet<>();
    for (final Method method : clazz.getMethods()) { // [A]
      final Path methodPath = AnnotationUtil.digestAnnotations(method, httpMethodAnnotations);
      final Path classPath = AnnotationUtil.getAnnotation(clazz, Path.class);
      if (httpMethodAnnotations.size() > 0) {
        for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations) // [S]
          register(httpMethodAnnotation, method, baseUri, classPath, methodPath, resourceInfos, clazz, singleton);

        httpMethodAnnotations.clear();
        changed = true;
      }
      else if (classPath != null || methodPath != null) {
        // FIXME: This is for the case of "JAX-RS 2.1 3.4.1: Sub-Resource Locator"
        // FIXME: Need to use a Digraph or RefDigraph
        final Class<?> returnType = method.getReturnType();
        if (!returnType.isAnnotation() && !returnType.isAnonymousClass() && !returnType.isArray() && !returnType.isEnum() && !returnType.isInterface() && !returnType.isPrimitive() && !returnType.isSynthetic() && returnType != Void.class && acceptPackage(returnType.getPackage())) {
          if (afterRegister == null)
            afterRegister = new ArrayList<>();

          afterRegister.add(() -> {
            for (final ResourceInfo resourceInfo : resourceInfos) {
              if (resourceInfo.getResourceClass() == returnType) {
                register(null, method, baseUri, classPath, methodPath, resourceInfos, clazz, singleton);
                break;
              }
            }
          });
        }
      }
    }

    return changed;
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