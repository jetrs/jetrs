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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.libj.lang.Classes;
import org.libj.lang.PackageLoader;
import org.libj.lang.PackageNotFoundException;
import org.libj.lang.ServiceLoaders;
import org.libj.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Bootstrap<R extends ArrayList<? extends Comparable<?>>> {
  static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
  static final Comparator<TypeComponent<?>> providerResourceComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.getPriority(), o2.getPriority()) : o1.getType().isAssignableFrom(o2.getType()) ? 1 : -1);
  private static final String[] excludeStartsWith = {"jdk.", "java.", "javax.", "com.sun.", "sun.", "org.w3c.", "org.xml.", "org.jvnet.", "org.joda.", "org.jcp.", "apple.security."};

  static boolean acceptPackage(final Package pkg) {
    for (int i = 0, i$ = excludeStartsWith.length; i < i$; ++i) // [A]
      if (pkg.getName().startsWith(excludeStartsWith[i]))
        return false;

    return true;
  }

  private static final boolean hasContextFields(final Class<?> clazz) {
    final Field[] fields = Classes.getDeclaredFieldsDeep(clazz, Component.injectableFieldPredicate);
    for (final Field field : fields) // [A]
      if (field.isAnnotationPresent(Context.class))
        return true;

    return false;
  }

  private final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents;
  private final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents;
  private final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents;
  private final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents;
  private final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents;

  Bootstrap(
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents
  ) {
    this.readerInterceptorComponents = readerInterceptorComponents;
    this.writerInterceptorComponents = writerInterceptorComponents;
    this.messageBodyReaderComponents = messageBodyReaderComponents;
    this.messageBodyWriterComponents = messageBodyWriterComponents;
    this.exceptionMapperComponents = exceptionMapperComponents;
  }

  @SuppressWarnings("unchecked")
  <T> boolean addResourceOrProvider(final ArrayList<Consumer<Set<Class<?>>>> afterAdds, final R resourceInfos, final Class<? extends T> clazz, final T singleton, final boolean scanned) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (ReaderInterceptor.class.isAssignableFrom(clazz))
      readerInterceptorComponents.add(new ReaderInterceptorComponent((Class<ReaderInterceptor>)clazz, (ReaderInterceptor)singleton));

    if (WriterInterceptor.class.isAssignableFrom(clazz))
      writerInterceptorComponents.add(new WriterInterceptorComponent((Class<WriterInterceptor>)clazz, (WriterInterceptor)singleton));

    if (MessageBodyReader.class.isAssignableFrom(clazz))
      messageBodyReaderComponents.add(new MessageBodyReaderComponent((Class<MessageBodyReader<?>>)clazz, (MessageBodyReader<?>)singleton));

    if (MessageBodyWriter.class.isAssignableFrom(clazz))
      messageBodyWriterComponents.add(new MessageBodyWriterComponent((Class<MessageBodyWriter<?>>)clazz, (MessageBodyWriter<?>)singleton));

    if (ExceptionMapper.class.isAssignableFrom(clazz))
      exceptionMapperComponents.add(new ExceptionMapperComponent((Class<ExceptionMapper<?>>)clazz, (ExceptionMapper<?>)singleton));

    return false;
  }

  private static boolean contains(final Set<String> disabledProviderClassNames, final Class<?> providerClass, final Class<?> interfaceClass) {
    if (disabledProviderClassNames.size() == 0)
      return false;

    Class<?> type = (Class<?>)Classes.getGenericInterfaceTypeArguments(providerClass, interfaceClass)[0];
    do
      if (disabledProviderClassNames.contains(type.getCanonicalName()))
        return true;
    while ((type = type.getSuperclass()) != null);
    return false;
  }

  @SuppressWarnings("rawtypes")
  private static void loadDefaultProviders(final Set<Object> singletons, final Set<Class<?>> classes) throws IOException {
    final String disableDefaultProviders = System.getProperty(CommonProperties.DISABLE_STANDARD_PROVIDER);
    final Set<String> disabledProviderClassNames;
    if (disableDefaultProviders != null) {
      final String[] classNames = Strings.split(disableDefaultProviders, ',');
      disabledProviderClassNames = new HashSet<>(classNames.length);
      Collections.addAll(disabledProviderClassNames, classNames);
      if (disabledProviderClassNames.contains("*"))
        return;
    }
    else {
      disabledProviderClassNames = Collections.EMPTY_SET;
    }

    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final HashSet<Class<?>> providerClasses = new HashSet<Class<?>>() {
      @Override
      public boolean add(final Class<?> e) {
        // Don't add the class if a subclass instance of it is already present in `singletons`
        if (singletons.size() > 0)
          for (final Object singleton : singletons) // [S]
            if (singleton != null)
              if (e.isAssignableFrom(singleton.getClass()))
                return false;

        // Don't add the class if a subclass of it is already present in `classes`
        if (classes.size() > 0)
          for (final Class<?> clazz : classes) // [S]
            if (clazz != null)
              if (e.isAssignableFrom(clazz))
                return false;

        return super.add(e);
      }
    };

    ServiceLoaders.load(ExceptionMapper.class, classLoader, (final Class<? super ExceptionMapper> e) -> {
      if (!contains(disabledProviderClassNames, e, ExceptionMapper.class))
        providerClasses.add(e);
    });
    ServiceLoaders.load(MessageBodyReader.class, classLoader, (final Class<? super MessageBodyReader> e) -> {
      if (!contains(disabledProviderClassNames, e, MessageBodyReader.class))
        providerClasses.add(e);
    });
    ServiceLoaders.load(MessageBodyWriter.class, classLoader, (final Class<? super MessageBodyWriter> e) -> {
      if (!contains(disabledProviderClassNames, e, MessageBodyWriter.class))
        providerClasses.add(e);
    });

    if (providerClasses.size() > 0) {
      for (final Class<?> providerClass : providerClasses) { // [S]
        try {
          if (hasContextFields(providerClass))
            classes.add(providerClass);
          else
            singletons.add(providerClass.getDeclaredConstructor().newInstance());

          if (logger.isDebugEnabled()) {
            final StringBuilder b = new StringBuilder();
            final Consumes c = providerClass.getAnnotation(Consumes.class);
            if (c != null)
              b.append(c).append(' ');

            final Produces p = providerClass.getAnnotation(Produces.class);
            if (p != null)
              b.append(p).append(' ');

            b.append("-> ").append(providerClass.getSimpleName());
            logger.debug(b.toString());
          }
        }
        catch (final Exception | ServiceConfigurationError e) {
          if (logger.isWarnEnabled()) { logger.warn("Failed to load provider " + providerClass, e); }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  void init(Set<Object> singletons, Set<Class<?>> classes, final R resourceInfos) throws IllegalAccessException, InstantiationException, InvocationTargetException, IOException {
    final ArrayList<Consumer<Set<Class<?>>>> afterAdds = new ArrayList<>();

    // Only scan the classpath if both singletons and classes are null
    if (singletons == null) {
      if (classes == null) {
        final HashSet<Class<?>>[] resourceClasses = new HashSet[1];
        final HashSet<Class<?>> initedClasses = new HashSet<>();
        final Predicate<Class<?>> initialize = (final Class<?> clazz) -> {
          if (!Modifier.isAbstract(clazz.getModifiers()) && !initedClasses.contains(clazz)) {
            try {
              if (addResourceOrProvider(afterAdds, resourceInfos, clazz, null, true)) {
                HashSet<Class<?>> resourceClass1 = resourceClasses[1];
                if (resourceClass1 == null)
                  resourceClass1 = resourceClasses[1] = new HashSet<>(2);

                resourceClass1.add(clazz);
              }
            }
            catch (final IllegalAccessException | InstantiationException e) {
              throw new ProviderInstantiationException(e);
            }
            catch (final InvocationTargetException e) {
              throw new ProviderInstantiationException(e.getCause());
            }
          }

          initedClasses.add(clazz);
          return false;
        };

        for (final Package pkg : Package.getPackages()) { // [A]
          if (acceptPackage(pkg)) {
            try {
              PackageLoader.getContextPackageLoader().loadPackage(pkg, initialize);
            }
            catch (final PackageNotFoundException e) {
              if (logger.isDebugEnabled()) { logger.debug(e.getMessage(), e); }
            }
          }
        }

        final Set<Class<?>> resourceClasses0 = resourceClasses[0];
        if (resourceClasses0 != null)
          for (int i = 0, i$ = afterAdds.size(); i < i$; ++i) // [RA]
            afterAdds.get(i).accept(resourceClasses0);

        singletons = new HashSet<>();
        classes = new HashSet<>();
      }
      else {
        singletons = new HashSet<>();
        classes = new HashSet<>(classes);
      }
    }
    else if (classes == null) {
      classes = new HashSet<>();
      singletons = new HashSet<>(singletons);
    }
    else {
      singletons = new HashSet<>(singletons);
      classes = new HashSet<>(classes);
    }

    loadDefaultProviders(singletons, classes);

    final int noClasses = classes.size();
    final boolean hasClasses = noClasses > 0;

    final int noSingletons = singletons.size();
    final boolean hasSingletons = noSingletons > 0;

    if (hasSingletons)
      for (final Object singleton : singletons) // [S]
        if (singleton != null)
          addResourceOrProvider(afterAdds, resourceInfos, singleton.getClass(), singleton, false);

    if (hasClasses)
      for (final Class<?> clazz : classes) // [S]
        if (clazz != null)
          addResourceOrProvider(afterAdds, resourceInfos, clazz, null, false);

    if (afterAdds.size() > 0) {
      final Set<Class<?>> resourceClasses;
      if (!hasSingletons) {
        resourceClasses = classes;
      }
      else if (hasClasses) {
        resourceClasses = new HashSet<>(noClasses + noSingletons);
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

    if (resourceInfos != null)
      resourceInfos.sort(null);

    exceptionMapperComponents.sort(providerResourceComparator);
    messageBodyReaderComponents.sort(providerResourceComparator);
    messageBodyWriterComponents.sort(providerResourceComparator);
  }
}