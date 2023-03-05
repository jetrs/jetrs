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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Singleton;
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

class Bootstrap<R extends ArrayList<? extends Comparable<?>>> {
  static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
  static final Comparator<TypeProviderFactory<?>> providerResourceComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.getPriority(), o2.getPriority()) : o1.getType().isAssignableFrom(o2.getType()) ? -1 : 1);
  private static final String[] excludeStartsWith = {"jdk.", "java.", "javax.", "com.sun.", "sun.", "org.w3c.", "org.xml.", "org.jvnet.", "org.joda.", "org.jcp.", "apple.security."};

  static boolean acceptPackage(final Package pkg) {
    for (int i = 0, i$ = excludeStartsWith.length; i < i$; ++i) // [A]
      if (pkg.getName().startsWith(excludeStartsWith[i]))
        return false;

    return true;
  }

  private final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;
  private final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  private final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories;
  private final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories;
  private final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories;

  Bootstrap(
    final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    this.readerInterceptorProviderFactories = readerInterceptorProviderFactories;
    this.writerInterceptorProviderFactories = writerInterceptorProviderFactories;
    this.messageBodyReaderProviderFactories = messageBodyReaderProviderFactories;
    this.messageBodyWriterProviderFactories = messageBodyWriterProviderFactories;
    this.exceptionMapperProviderFactories = exceptionMapperProviderFactories;
  }

  @SuppressWarnings("unchecked")
  <T>boolean addResourceOrProvider(final ArrayList<Consumer<Set<Class<?>>>> afterAdds, final R resourceInfos, final Class<? extends T> clazz, final T singleton, final boolean scanned) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (AnnotationUtil.isAnnotationPresent(clazz, Provider.class)) {
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
      if (logger.isWarnEnabled()) logger.warn("Ignored resource class " + clazz.getName() + " due to absent @Provider annotation");
    }

    return false;
  }

  @SuppressWarnings({"null", "unchecked"})
  void init(final Set<Object> singletons, final Set<Class<?>> classes, final R resourceInfos) throws IllegalAccessException, InstantiationException, InvocationTargetException, PackageNotFoundException, IOException {
    final ArrayList<Consumer<Set<Class<?>>>> afterAdds = new ArrayList<>();
    if (singletons != null || classes != null) {
      final boolean hasSingletons = singletons.size() > 0;
      final boolean hasClasses = classes.size() > 0;
      if (hasSingletons) {
        for (final Object singleton : singletons) { // [S]
          if (singleton != null) {
            if (logger.isWarnEnabled() && !singleton.getClass().isAnnotationPresent(Singleton.class)) logger.warn("Object of class " + singleton.getClass().getName() + " without @Singleton annotation is member of Application.getSingletons()");
            addResourceOrProvider(afterAdds, resourceInfos, singleton.getClass(), singleton, false);
          }
        }
      }

      if (hasClasses)
        for (final Class<?> cls : classes) // [S]
          if (cls != null)
            addResourceOrProvider(afterAdds, resourceInfos, cls, null, false);

      if (afterAdds.size() > 0) {
        final Set<Class<?>> resourceClasses;
        if (!hasSingletons) {
          resourceClasses = classes;
        }
        else if (hasClasses) {
          resourceClasses = new HashSet<>(classes.size() + singletons.size());
          resourceClasses.addAll(classes);
          for (final Object singleton : singletons) // [S]
            resourceClasses.add(singleton.getClass());
        }
        else {
          resourceClasses = null;
        }

        for (int i = 0, i$ = afterAdds.size(); i < i$; ++i) // [RA]
          afterAdds.get(i).accept(resourceClasses);
      }
    }
    else {
      final Set<Class<?>>[] resourceClasses = new Set[1];
      final Set<Class<?>> initedClasses = new HashSet<>();
      final Predicate<Class<?>> initialize = cls -> {
        if (!Modifier.isAbstract(cls.getModifiers()) && !initedClasses.contains(cls)) {
          try {
            if (addResourceOrProvider(afterAdds, resourceInfos, cls, null, true)) {
              Set<Class<?>> resourceClass1 = resourceClasses[1];
              if (resourceClass1 == null)
                resourceClass1 = resourceClasses[1] = new HashSet<>(2);

              resourceClass1.add(cls);
            }
          }
          catch (final IllegalAccessException | InstantiationException e) {
            throw new ProviderInstantiationException(e);
          }
          catch (final InvocationTargetException e) {
            throw new ProviderInstantiationException(e.getCause());
          }
        }

        initedClasses.add(cls);
        return false;
      };

      for (final Package pkg : Package.getPackages()) // [A]
        if (acceptPackage(pkg))
          PackageLoader.getContextPackageLoader().loadPackage(pkg, initialize);

      final Set<Class<?>> resourceClasses0 = resourceClasses[0];
      if (resourceClasses0 != null)
        for (int i = 0, i$ = afterAdds.size(); i < i$; ++i) // [RA]
          afterAdds.get(i).accept(resourceClasses0);
    }

    if (resourceInfos != null)
      resourceInfos.sort(null);

    exceptionMapperProviderFactories.sort(providerResourceComparator);
    messageBodyReaderProviderFactories.sort(providerResourceComparator);
    messageBodyWriterProviderFactories.sort(providerResourceComparator);
  }
}