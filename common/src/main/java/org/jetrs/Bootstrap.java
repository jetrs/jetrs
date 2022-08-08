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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.libj.lang.PackageLoader;
import org.libj.lang.PackageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Bootstrap<R extends Comparable<? super R>> {
  static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
  static final Comparator<TypeProviderFactory<?>> providerResourceComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.getPriority(), o2.getPriority()) : o1.getType().isAssignableFrom(o2.getType()) ? -1 : 1);
  private static final String[] excludeStartsWith = {"jdk.", "java.", "javax.", "com.sun.", "sun.", "org.w3c.", "org.xml.", "org.jvnet.", "org.joda.", "org.jcp.", "apple.security."};

  static boolean acceptPackage(final Package pkg) {
    for (int i = 0; i < excludeStartsWith.length; ++i) // [A]
      if (pkg.getName().startsWith(excludeStartsWith[i]))
        return false;

    return true;
  }

  private final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;
  private final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  private final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories;
  private final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories;
  private final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories;

  Bootstrap(
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    this.readerInterceptorProviderFactories = readerInterceptorProviderFactories;
    this.writerInterceptorProviderFactories = writerInterceptorProviderFactories;
    this.messageBodyReaderProviderFactories = messageBodyReaderProviderFactories;
    this.messageBodyWriterProviderFactories = messageBodyWriterProviderFactories;
    this.exceptionMapperProviderFactories = exceptionMapperProviderFactories;
  }

  @SuppressWarnings("unchecked")
  <T>boolean addResourceOrProvider(final List<Consumer<Set<Class<?>>>> afterAdds, final List<R> resources, final Class<? extends T> clazz, final T singleton, final boolean scanned) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (clazz.isAnnotationPresent(Provider.class)) {
      if (ReaderInterceptor.class.isAssignableFrom(clazz))
        readerInterceptorProviderFactories.add(new ReaderInterceptorProviderFactory((Class<ReaderInterceptor>)clazz, (ReaderInterceptor)singleton));

      if (WriterInterceptor.class.isAssignableFrom(clazz))
        writerInterceptorProviderFactories.add(new WriterInterceptorProviderFactory((Class<WriterInterceptor>)clazz, (WriterInterceptor)singleton));

      if (MessageBodyReader.class.isAssignableFrom(clazz))
        messageBodyReaderProviderFactories.add(new MessageBodyReaderProviderFactory((Class<MessageBodyReader<?>>)clazz, (MessageBodyReader<?>)singleton));

      if (MessageBodyWriter.class.isAssignableFrom(clazz))
        messageBodyWriterProviderFactories.add(new MessageBodyWriterProviderFactory((Class<MessageBodyWriter<?>>)clazz, (MessageBodyWriter<?>)singleton));

      if (ExceptionMapper.class.isAssignableFrom(clazz))
        exceptionMapperProviderFactories.add(new ExceptionMapperProviderFactory((Class<ExceptionMapper<?>>)clazz, (ExceptionMapper<?>)singleton));
    }
    else if (!scanned) {
      logger.warn("Ignored resource class " + clazz.getName() + " due to absent @Provider annotation");
    }

    return false;
  }

  @SuppressWarnings("unchecked")
  void init(final Set<?> singletons, final Set<Class<?>> classes, final List<R> resources) throws IllegalAccessException, InstantiationException, InvocationTargetException, PackageNotFoundException, IOException {
    final List<Consumer<Set<Class<?>>>> afterAdds = new ArrayList<>();
    if (singletons != null || classes != null) {
      if (singletons != null)
        for (final Object singleton : singletons) // [S]
          if (singleton != null)
            addResourceOrProvider(afterAdds, resources, singleton.getClass(), singleton, false);

      if (classes != null)
        for (final Class<?> cls : classes) // [S]
          if (cls != null)
            addResourceOrProvider(afterAdds, resources, cls, null, false);

      if (afterAdds.size() > 0) {
        final Set<Class<?>> resourceClasses;
        if (singletons == null) {
          resourceClasses = classes;
        }
        else if (classes != null) {
          resourceClasses = new HashSet<>(classes.size() + singletons.size());
          resourceClasses.addAll(classes);
          for (final Object singleton : singletons) // [S]
            resourceClasses.add(singleton.getClass());
        }
        else {
          resourceClasses = null;
        }

        for (int i = 0, len = afterAdds.size(); i < len; ++i) // [L]
          afterAdds.get(i).accept(resourceClasses);
      }
    }
    else {
      final Set<Class<?>>[] resourceClasses = new Set[1];
      final Set<Class<?>> initedClasses = new HashSet<>();
      final Predicate<Class<?>> initialize = t -> {
        if (!Modifier.isAbstract(t.getModifiers()) && !initedClasses.contains(t)) {
          try {
            if (addResourceOrProvider(afterAdds, resources, t, null, true))
              (resourceClasses[1] == null ? resourceClasses[1] = new HashSet<>() : resourceClasses[1]).add(t);
          }
          catch (final IllegalAccessException | InstantiationException e) {
            throw new ProviderInstantiationException(e);
          }
          catch (final InvocationTargetException e) {
            throw new ProviderInstantiationException(e.getCause());
          }
        }

        initedClasses.add(t);
        return false;
      };

      for (final Package pkg : Package.getPackages()) // [A]
        if (acceptPackage(pkg))
          PackageLoader.getContextPackageLoader().loadPackage(pkg, initialize);

      final Set<Class<?>> resourceClasses0 = resourceClasses[0];
      if (resourceClasses0 != null)
        for (int i = 0, len = afterAdds.size(); i < len; ++i) // [L]
          afterAdds.get(i).accept(resourceClasses0);
    }

    if (resources != null)
      resources.sort(null);

    exceptionMapperProviderFactories.sort(providerResourceComparator);
    messageBodyReaderProviderFactories.sort(providerResourceComparator);
    messageBodyWriterProviderFactories.sort(providerResourceComparator);
  }
}