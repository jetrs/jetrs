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

package org.jetrs.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.libj.lang.PackageLoader;
import org.libj.lang.PackageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap<R> {
  protected static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
  private static final String[] excludeStartsWith = {"jdk", "java", "javax", "com.sun", "sun", "org.w3c", "org.xml", "org.jvnet", "org.joda", "org.jcp", "apple.security"};

  private static boolean acceptPackage(final Package pkg) {
    for (int i = 0; i < excludeStartsWith.length; ++i)
      if (pkg.getName().startsWith(excludeStartsWith[i] + "."))
        return false;

    return true;
  }

  @SuppressWarnings("unchecked")
  protected <T>void addResourceOrProvider(final MultivaluedMap<String,R> resources, final List<ExceptionMappingProviderResource> exceptionMappers, final List<EntityReaderProviderResource> entityReaders, final List<EntityWriterProviderResource> entityWriters, final List<ProviderResource<ContainerRequestFilter>> requestFilters, final List<ProviderResource<ContainerResponseFilter>> responseFilters, final List<ReaderInterceptorEntityProviderResource> readerInterceptors, final List<WriterInterceptorEntityProviderResource> writerInterceptors, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders, final Class<? extends T> clazz, T singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (clazz.isAnnotationPresent(Provider.class)) {
      if (MessageBodyReader.class.isAssignableFrom(clazz))
        entityReaders.add(new EntityReaderProviderResource((Class<MessageBodyReader<?>>)clazz, (MessageBodyReader<?>)singleton));

      if (MessageBodyWriter.class.isAssignableFrom(clazz))
        entityWriters.add(new EntityWriterProviderResource((Class<MessageBodyWriter<?>>)clazz, (MessageBodyWriter<?>)singleton));

      if (ReaderInterceptor.class.isAssignableFrom(clazz))
        readerInterceptors.add(new ReaderInterceptorEntityProviderResource((Class<ReaderInterceptor>)clazz, (ReaderInterceptor)singleton));

      if (WriterInterceptor.class.isAssignableFrom(clazz))
        writerInterceptors.add(new WriterInterceptorEntityProviderResource((Class<WriterInterceptor>)clazz, (WriterInterceptor)singleton));

      if (ExceptionMapper.class.isAssignableFrom(clazz))
        exceptionMappers.add(new ExceptionMappingProviderResource((Class<ExceptionMapper<?>>)clazz, (ExceptionMapper<?>)singleton));

      if (ParamConverterProvider.class.isAssignableFrom(clazz))
        paramConverterProviders.add(new ProviderResource<>((Class<ParamConverterProvider>)clazz, (ParamConverterProvider)singleton));

      if (ContainerRequestFilter.class.isAssignableFrom(clazz))
        requestFilters.add(new ProviderResource<>((Class<ContainerRequestFilter>)clazz, (ContainerRequestFilter)singleton));

      if (ContainerResponseFilter.class.isAssignableFrom(clazz))
        responseFilters.add(new ProviderResource<>((Class<ContainerResponseFilter>)clazz, (ContainerResponseFilter)singleton));
    }
  }

  public final void init(final Set<?> singletons, final Set<Class<?>> classes, final MultivaluedMap<String,R> resources, final List<ExceptionMappingProviderResource> exceptionMappers, final List<EntityReaderProviderResource> entityReaders, final List<EntityWriterProviderResource> entityWriters, final List<ProviderResource<ContainerRequestFilter>> requestFilters, final List<ProviderResource<ContainerResponseFilter>> responseFilters, final List<ReaderInterceptorEntityProviderResource> readerInterceptors, final List<WriterInterceptorEntityProviderResource> writerInterceptors, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IllegalAccessException, InstantiationException, InvocationTargetException, PackageNotFoundException, IOException {
    if (singletons != null || classes != null) {
      if (singletons != null)
        for (final Object singleton : singletons)
          if (singleton != null)
            addResourceOrProvider(resources, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, readerInterceptors, writerInterceptors, paramConverterProviders, singleton.getClass(), singleton);

      if (classes != null)
        for (final Class<?> cls : classes)
          if (cls != null)
            addResourceOrProvider(resources, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, readerInterceptors, writerInterceptors, paramConverterProviders, cls, null);
    }
    else {
      final Predicate<Class<?>> initialize = new Predicate<Class<?>>() {
        private final Set<Class<?>> loadedClasses = new HashSet<>();

        @Override
        public boolean test(final Class<?> t) {
          if (!Modifier.isAbstract(t.getModifiers()) && !loadedClasses.contains(t)) {
            try {
              addResourceOrProvider(resources, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, readerInterceptors, writerInterceptors, paramConverterProviders, t, null);
            }
            catch (final IllegalAccessException | InstantiationException e) {
              throw new ProviderInstantiationException(e);
            }
            catch (final InvocationTargetException e) {
              throw new ProviderInstantiationException(e.getCause());
            }
          }

          loadedClasses.add(t);
          return false;
        }
      };

      for (final Package pkg : Package.getPackages())
        if (acceptPackage(pkg))
          PackageLoader.getContextPackageLoader().loadPackage(pkg, initialize);
    }
  }
}