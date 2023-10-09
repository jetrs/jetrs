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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.libj.lang.Classes;
import org.libj.lang.ServiceLoaders;
import org.libj.lang.Strings;
import org.libj.util.TransSet;
import org.libj.util.UnmodifiableCompositeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Components implements Cloneable {
  private static final Logger logger = LoggerFactory.getLogger(Components.class);
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

  private static final HashSet<Class<?>> defaultProviderClasses = new HashSet<Class<?>>() {
    @Override
    public boolean add(final Class<?> e) {
      // Don't add the class if a subclass instance of it is already present in `singletons` // FIXME: "subclass"?
      return contains(e) || super.add(e);
    }
  };

  @SuppressWarnings("rawtypes")
  private static void loadDefaultProviders() throws IOException {
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
    ServiceLoaders.load(ExceptionMapper.class, classLoader, (final Class<? super ExceptionMapper> e) -> {
      if (!contains(disabledProviderClassNames, e, ExceptionMapper.class))
        defaultProviderClasses.add(e);
    });
    ServiceLoaders.load(MessageBodyReader.class, classLoader, (final Class<? super MessageBodyReader> e) -> {
      if (!contains(disabledProviderClassNames, e, MessageBodyReader.class))
        defaultProviderClasses.add(e);
    });
    ServiceLoaders.load(MessageBodyWriter.class, classLoader, (final Class<? super MessageBodyWriter> e) -> {
      if (!contains(disabledProviderClassNames, e, MessageBodyWriter.class))
        defaultProviderClasses.add(e);
    });
  }

  static {
    try {
      loadDefaultProviders();
    }
    catch (final IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  Components() {
    if (defaultProviderClasses.size() > 0) {
      for (final Class<?> defaultProviderClass : defaultProviderClasses) { // [S]
        try {
          if (register(defaultProviderClass, hasContextFields(defaultProviderClass) ? null : defaultProviderClass.getConstructor().newInstance(), true, null, -1) && logger.isDebugEnabled()) {
            final StringBuilder b = new StringBuilder();
            final Consumes c = defaultProviderClass.getAnnotation(Consumes.class);
            if (c != null)
              b.append(c).append(' ');

            final Produces p = defaultProviderClass.getAnnotation(Produces.class);
            if (p != null)
              b.append(p).append(' ');

            b.append("-> ").append(defaultProviderClass.getSimpleName());
            logger.debug(b.toString());
          }
        }
        catch (final IllegalArgumentException e) {
        }
        catch (final Exception | ServiceConfigurationError e) {
          if (logger.isWarnEnabled()) { logger.warn("Failed to load provider " + defaultProviderClass, e); }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  <T> boolean register(final Class<? extends T> clazz, final T instance, final boolean isDefaultProvider, final Map<Class<?>,Integer> contracts, final int priority) {
    boolean added = false;
    if (ReaderInterceptor.class.isAssignableFrom(clazz)) {
      if (readerInterceptorComponents == null)
        readerInterceptorComponents = new ComponentSet.Typed<>();

      readerInterceptorComponents.register(new ReaderInterceptorComponent((Class<ReaderInterceptor>)clazz, (ReaderInterceptor)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (WriterInterceptor.class.isAssignableFrom(clazz)) {
      if (writerInterceptorComponents == null)
        writerInterceptorComponents = new ComponentSet.Typed<>();

      writerInterceptorComponents.register(new WriterInterceptorComponent((Class<WriterInterceptor>)clazz, (WriterInterceptor)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (MessageBodyReader.class.isAssignableFrom(clazz)) {
      if (messageBodyReaderComponents == null)
        messageBodyReaderComponents = new ComponentSet.Typed<>();

      messageBodyReaderComponents.register(new MessageBodyReaderComponent((Class<MessageBodyReader<?>>)clazz, (MessageBodyReader<?>)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (MessageBodyWriter.class.isAssignableFrom(clazz)) {
      if (messageBodyWriterComponents == null)
        messageBodyWriterComponents = new ComponentSet.Typed<>();

      messageBodyWriterComponents.register(new MessageBodyWriterComponent((Class<MessageBodyWriter<?>>)clazz, (MessageBodyWriter<?>)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    if (ExceptionMapper.class.isAssignableFrom(clazz)) {
      if (exceptionMapperComponents == null)
        exceptionMapperComponents = new ComponentSet.Typed<>();

      exceptionMapperComponents.register(new ExceptionMapperComponent((Class<ExceptionMapper<?>>)clazz, (ExceptionMapper<?>)instance, isDefaultProvider, contracts, priority));
      added = true;
    }

    return added;
  }

  private ComponentSet<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents;
  private ComponentSet<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents;
  private ComponentSet<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents;
  private ComponentSet<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents;
  private ComponentSet<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents;

  final ComponentSet<MessageBodyComponent<ReaderInterceptor>> getReaderInterceptorComponents() {
    return readerInterceptorComponents != null ? readerInterceptorComponents : ComponentSet.EMPTY;
  }

  final ComponentSet<MessageBodyComponent<WriterInterceptor>> getWriterInterceptorComponents() {
    return writerInterceptorComponents != null ? writerInterceptorComponents : ComponentSet.EMPTY;
  }

  final ComponentSet<MessageBodyComponent<MessageBodyReader<?>>> getMessageBodyReaderComponents() {
    return messageBodyReaderComponents != null ? messageBodyReaderComponents : ComponentSet.EMPTY;
  }

  final ComponentSet<MessageBodyComponent<MessageBodyWriter<?>>> getMessageBodyWriterComponents() {
    return messageBodyWriterComponents != null ? messageBodyWriterComponents : ComponentSet.EMPTY;
  }

  final ComponentSet<TypeComponent<ExceptionMapper<?>>> getExceptionMapperComponents() {
    return exceptionMapperComponents != null ? exceptionMapperComponents : ComponentSet.EMPTY;
  }

  int toCompositeSetArray(final ComponentSet<?>[] target, int index) {
    target[index++] = readerInterceptorComponents;
    target[index++] = writerInterceptorComponents;
    target[index++] = messageBodyReaderComponents;
    target[index++] = messageBodyWriterComponents;
    target[index++] = exceptionMapperComponents;
    return index;
  }

  int noComponentTypes() {
    return 5;
  }

  private UnmodifiableCompositeSet<Component<?>> compositeSet;

  @SuppressWarnings({"rawtypes", "unchecked"})
  private UnmodifiableCompositeSet<Component<?>> compositeSet() {
    if (compositeSet == null) {
      final ComponentSet[] target = new ComponentSet[noComponentTypes()];
      toCompositeSetArray(target, 0);
      compositeSet = new UnmodifiableCompositeSet<>(target);
    }

    return compositeSet;
  }

  private Set<Class<?>> classes;

  final Set<Class<?>> classes() {
    return classes == null ? classes = new TransSet<>(compositeSet(), (final Component<?> c) -> c.clazz, null) : classes;
  }

  private Set<Object> instances;

  final Set<Object> instances() {
    return instances == null ? instances = new TransSet<>(compositeSet(), (final Component<?> c) -> c.instance, null) : instances;
  }

  boolean contains(final Class<?> componentClass) {
    if (ReaderInterceptor.class.isAssignableFrom(componentClass))
      return readerInterceptorComponents.containsComponent(componentClass);

    if (WriterInterceptor.class.isAssignableFrom(componentClass))
      return writerInterceptorComponents.containsComponent(componentClass);

    if (MessageBodyReader.class.isAssignableFrom(componentClass))
      return messageBodyReaderComponents.containsComponent(componentClass);

    if (MessageBodyWriter.class.isAssignableFrom(componentClass))
      return messageBodyWriterComponents.containsComponent(componentClass);

    if (ExceptionMapper.class.isAssignableFrom(componentClass))
      return exceptionMapperComponents.containsComponent(componentClass);

    return false;
  }

  boolean contains(final Object component) {
    final Class<?> componentClass = component.getClass();
    if (ReaderInterceptor.class.isAssignableFrom(componentClass))
      return readerInterceptorComponents.containsComponent(component);

    if (WriterInterceptor.class.isAssignableFrom(componentClass))
      return writerInterceptorComponents.containsComponent(component);

    if (MessageBodyReader.class.isAssignableFrom(componentClass))
      return messageBodyReaderComponents.containsComponent(component);

    if (MessageBodyWriter.class.isAssignableFrom(componentClass))
      return messageBodyWriterComponents.containsComponent(component);

    if (ExceptionMapper.class.isAssignableFrom(componentClass))
      return exceptionMapperComponents.containsComponent(component);

    return false;
  }

  Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    if (ReaderInterceptor.class.isAssignableFrom(componentClass))
      return readerInterceptorComponents.getContracts(componentClass);

    if (WriterInterceptor.class.isAssignableFrom(componentClass))
      return writerInterceptorComponents.getContracts(componentClass);

    if (MessageBodyReader.class.isAssignableFrom(componentClass))
      return messageBodyReaderComponents.getContracts(componentClass);

    if (MessageBodyWriter.class.isAssignableFrom(componentClass))
      return messageBodyWriterComponents.getContracts(componentClass);

    if (ExceptionMapper.class.isAssignableFrom(componentClass))
      return exceptionMapperComponents.getContracts(componentClass);

    return null;
  }

  boolean isEmpty() {
    return readerInterceptorComponents == null && writerInterceptorComponents == null && messageBodyReaderComponents == null && messageBodyWriterComponents == null && exceptionMapperComponents == null;
  }

  @Override
  protected Components clone() {
    try {
      final Components clone = (Components)super.clone();
      if (readerInterceptorComponents != null)
        clone.readerInterceptorComponents = readerInterceptorComponents.clone();

      if (writerInterceptorComponents != null)
        clone.writerInterceptorComponents = writerInterceptorComponents.clone();

      if (messageBodyReaderComponents != null)
        clone.messageBodyReaderComponents = messageBodyReaderComponents.clone();

      if (messageBodyWriterComponents != null)
        clone.messageBodyWriterComponents = messageBodyWriterComponents.clone();

      if (exceptionMapperComponents != null)
        clone.exceptionMapperComponents = exceptionMapperComponents.clone();

      clone.classes = null;
      clone.instances = null;
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}