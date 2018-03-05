/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.lib4j.lang.PackageLoader;
import org.lib4j.lang.PackageNotFoundException;
import org.libx4j.xrs.server.core.ContextInjector;
import org.libx4j.xrs.server.ext.ProvidersImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RestHttpServlet extends HttpServlet {
  private static final long serialVersionUID = 6825431027711735886L;
  private static final Logger logger = LoggerFactory.getLogger(RestHttpServlet.class);
  private static final String[] excludeStartsWith = {"jdk", "java", "javax", "com.sun", "sun", "org.w3c", "org.xml", "org.jvnet", "org.joda", "org.jcp", "apple.security"};

  private static boolean acceptPackage(final Package pkg) {
    for (int i = 0; i < excludeStartsWith.length; i++)
      if (pkg.getName().startsWith(excludeStartsWith[i] + "."))
        return false;

    return true;
  }

  private static <T>void addResourceProvider(final MultivaluedMap<String,ResourceManifest> registry, final List<ExceptionMapper<?>> exceptionMappers, final List<MessageBodyReader<?>> entityReaders, final List<MessageBodyWriter<?>> entityWriters, final List<ContainerRequestFilter> requestFilters, final List<ContainerResponseFilter> responseFilters, final List<ParamConverterProvider> paramConverterProviders, final Class<? extends T> cls, T singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    if (isRootResource(cls)) {
      for (final Method method : cls.getMethods()) {
        final Set<HttpMethod> httpMethodAnnotations = new HashSet<HttpMethod>(); // FIXME: Can this be done without a Collection?
        final Annotation[] annotations = method.getAnnotations();
        for (final Annotation annotation : annotations) {
          final HttpMethod httpMethodAnnotation = annotation.annotationType().getAnnotation(HttpMethod.class);
          if (httpMethodAnnotation != null)
            httpMethodAnnotations.add(httpMethodAnnotation);
        }

        for (final HttpMethod httpMethodAnnotation : httpMethodAnnotations) {
          ContextInjector.allowsInjectableClass(Field.class, cls);
          final ResourceManifest manifest = new ResourceManifest(httpMethodAnnotation, method, singleton);
          logger.info(httpMethodAnnotation.value() + " " + manifest.getPathPattern().getPattern().toString() + " -> " + cls.getSimpleName() + "." + method.getName() + "()");
          registry.add(manifest.getHttpMethod().value().toUpperCase(), manifest);
        }
      }
    }
    else if (cls.isAnnotationPresent(Provider.class)) {
      // Automatically discovered @Provider(s) are singletons
      // FIXME: Have to inject here! Constructor may have @Context stuff
      if (singleton == null)
        singleton = cls.getDeclaredConstructor().newInstance();

      for (final Class<?> inter : cls.getInterfaces()) {
        if (inter == MessageBodyReader.class) {
          entityReaders.add((MessageBodyReader<?>)singleton);
        }
        else if (inter == ExceptionMapper.class) {
          exceptionMappers.add((ExceptionMapper<?>)singleton);
        }
        else if (inter == MessageBodyWriter.class) {
          entityWriters.add((MessageBodyWriter<?>)singleton);
        }
        else if (inter == ParamConverterProvider.class) {
          paramConverterProviders.add((ParamConverterProvider)singleton);
        }
        else if (inter == ContainerRequestFilter.class) {
          requestFilters.add((ContainerRequestFilter)singleton);
        }
        else if (inter == ContainerResponseFilter.class) {
          responseFilters.add((ContainerResponseFilter)singleton);
        }
        else {
          throw new UnsupportedOperationException("Unsupported @Provider of type: " + cls.getName());
        }
      }
    }
  }

  private ResourceContext resourceContext;

  protected ResourceContext getResourceContext() {
    return resourceContext;
  }

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
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    final MultivaluedMap<String,ResourceManifest> registry = new MultivaluedHashMap<String,ResourceManifest>();
    final List<ParamConverterProvider> paramConverterProviders = new ArrayList<ParamConverterProvider>();
    final List<ExceptionMapper<?>> exceptionMappers = new ArrayList<ExceptionMapper<?>>();
    final List<MessageBodyReader<?>> entityReaders = new ArrayList<MessageBodyReader<?>>();
    final List<MessageBodyWriter<?>> entityWriters = new ArrayList<MessageBodyWriter<?>>();
    final List<ContainerRequestFilter> requestFilters = new ArrayList<ContainerRequestFilter>();
    final List<ContainerResponseFilter> responseFilters = new ArrayList<ContainerResponseFilter>();

    final String applicationSpec = getInitParameter("javax.ws.rs.Application");
    if (applicationSpec != null) {
      try {
        final Application application = (Application)Class.forName(applicationSpec).getDeclaredConstructor().newInstance();
        final Set<?> singletons = application.getSingletons();
        if (singletons != null)
          for (final Object singleton : singletons)
            addResourceProvider(registry, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, paramConverterProviders, singleton.getClass(), singleton);

        final Set<Class<?>> classes = application.getClasses();
        if (classes != null)
          for (final Class<?> cls : classes)
            addResourceProvider(registry, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, paramConverterProviders, cls, null);
      }
      catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new WebApplicationException(e);
      }
    }
    else {
      final Predicate<Class<?>> initialize = new Predicate<Class<?>>() {
        private final Set<Class<?>> loadedClasses = new HashSet<Class<?>>();
        @Override
        public boolean test(final Class<?> t) {
          try {
            if (!Modifier.isAbstract(t.getModifiers()) && !loadedClasses.contains(t))
              addResourceProvider(registry, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, paramConverterProviders, t, null);
          }
          catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new WebApplicationException(e);
          }

          loadedClasses.add(t);
          return false;
        }
      };

      try {
        for (final Package pkg : Package.getPackages())
          if (acceptPackage(pkg))
            PackageLoader.getSystemContextPackageLoader().loadPackage(pkg, initialize);
      }
      catch (final PackageNotFoundException e) {
      }
    }

    this.resourceContext = new ResourceContext(registry, new ContainerFilters(requestFilters, responseFilters), new ProvidersImpl(exceptionMappers, entityReaders, entityWriters), paramConverterProviders);
  }
}