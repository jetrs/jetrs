/* Copyright (c) 2022 JetRS
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.libj.lang.Classes;
import org.libj.util.function.Throwing;
import org.libj.util.function.ThrowingPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <P> The type parameter of the associated properties.
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0 [9.2]</a>
 */
abstract class RequestContext<P> extends InterceptorContextImpl<P> {
  private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);
  private static final Comparator<Constructor<?>> parameterCountComparator = Comparator.comparingInt(c -> -c.getParameterCount());
  @SuppressWarnings("rawtypes")
  private static final Map<Class<?>,Constructor[]> classToConstructors = new HashMap<>();

  // Gets the constructor with most args [JAX-RS 2.1 3.1.2]
  @SuppressWarnings("unchecked")
  private static <T>Constructor<T>[] getConstructors(final Class<T> clazz) {
    Constructor<T>[] constructors = classToConstructors.get(clazz);
    if (constructors == null)
      classToConstructors.put(clazz, constructors = (Constructor<T>[])clazz.getConstructors());

    Arrays.sort(constructors, parameterCountComparator);
    return constructors;
  }

  private final RuntimeContext runtimeContext;
  private final Request request;
  private final ProvidersImpl providers;

  RequestContext(final PropertiesAdapter<P> propertiesAdapter, final RuntimeContext runtimeContext, final Request request) {
    super(propertiesAdapter);
    this.method = request.getMethod();
    this.runtimeContext = runtimeContext;
    this.request = request;
    this.providers = new ProvidersImpl(this);
  }

  public final Request getRequest() {
    return request;
  }

  public final void abortWith(final Response response) {
    throw new AbortFilterChainException(response);
  }

  public final List<MediaType> getAcceptableMediaTypes() {
    return getHttpHeaders().getAcceptableMediaTypes();
  }

  public final List<Locale> getAcceptableLanguages() {
    return getHttpHeaders().getAcceptableLanguages();
  }

  private String method;

  public final String getMethod() {
    return method;
  }

  public final void setMethod(final String method) {
    this.method = method;
  }

  public final Map<String,Cookie> getCookies() {
    return getHttpHeaders().getCookies();
  }

  private boolean readerInterceptorCalled = false;

  ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> getReaderInterceptorFactoryList() {
    if (readerInterceptorCalled)
      throw new IllegalStateException();

    readerInterceptorCalled = true;
    return runtimeContext.getReaderInterceptorProviderFactories();
  }

  private boolean writerInterceptorCalled = false;

  ArrayList<MessageBodyProviderFactory<WriterInterceptor>> getWriterInterceptorFactoryList() {
    if (writerInterceptorCalled)
      throw new IllegalStateException();

    writerInterceptorCalled = true;
    return runtimeContext.getWriterInterceptorProviderFactories();
  }

  private boolean messageBodyReaderCalled = false;

  ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> getMessageBodyReaderFactoryList() {
    if (messageBodyReaderCalled)
      throw new IllegalStateException();

    messageBodyReaderCalled = true;
    return runtimeContext.getMessageBodyReaderProviderFactories();
  }

  private boolean messageBodyWriterCalled = false;

  ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> getMessageBodyWriterFactoryList() {
    if (messageBodyWriterCalled)
      throw new IllegalStateException();

    messageBodyWriterCalled = true;
    return runtimeContext.getMessageBodyWriterProviderFactories();
  }

  private boolean exceptionMapperProviderCalled = false;

  ArrayList<TypeProviderFactory<ExceptionMapper<?>>> getExceptionMapperProviderFactoryList() {
    if (exceptionMapperProviderCalled)
      throw new IllegalStateException();

    exceptionMapperProviderCalled = true;
    return runtimeContext.getExceptionMapperProviderFactories();
  }

  final Providers getProviders() {
    return providers;
  }

  Annotation findInjectableAnnotation(final Annotation[] annotations, final boolean isResource) {
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType() == Context.class)
        return annotation;

    return null;
  }

  @SuppressWarnings("unchecked")
  <T>T findInjectableContextValue(final Class<T> clazz) {
    if (Request.class.isAssignableFrom(clazz))
      return (T)request;

    if (Providers.class.isAssignableFrom(clazz))
      return (T)providers;

    return null;
  }

  private static List<Object> makeCacheKey(final Annotation[] annotations, final Class<?> clazz, final Type type) {
    final Object[] key = new Object[annotations.length + 2];
    int i = 0;
    while (i < annotations.length)
      key[i] = annotations[i++];

    key[i++] = clazz;
    key[i] = type;
    return Arrays.asList(key);
  }

  private Map<List<Object>,Object> injectedValueCache;

  @SuppressWarnings("unchecked")
  private <T>T findInjectableValueFromCache(final AnnotatedElement element, final Annotation[] annotations, final Class<T> clazz, final Type type) throws IOException {
    if (injectedValueCache == null) {
      injectedValueCache = new HashMap<>();
    }
    else {
      final List<Object> key = makeCacheKey(annotations, clazz, type);
      final Object instance = injectedValueCache.get(key);
      if (instance != null)
        return (T)instance;
    }

    final T instance = findInjectableValue(element, annotations, clazz, type);
    if (instance == null)
      return null;

    final List<Object> key = makeCacheKey(annotations, clazz, type);
    injectedValueCache.put(key, instance);
    return instance;
  }

  final Object invokeMethod(final Object obj, final Method method) throws IllegalAccessException, InvocationTargetException, IOException {
    if (method.getParameterCount() == 0)
      return method.invoke(obj);

    final Parameter[] parameters = method.getParameters();
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final Type[] genericParameterTypes = method.getGenericParameterTypes();
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    final Object[] arguments = new Object[parameters.length];
    for (int i = 0; i < parameters.length; ++i) { // [A]
      final Object arg = arguments[i] = findInjectableValueFromCache(parameters[i], parameterAnnotations[i], parameterTypes[i], genericParameterTypes[i]);
      if (arg instanceof Exception)
        throw new BadRequestException((Exception)arg);
    }

    return method.invoke(obj, arguments);
  }

  <T>T findInjectableValue(final AnnotatedElement element, final Annotation[] annotations, final Class<T> clazz, final Type type) throws IOException {
    return findInjectableContextValue(clazz);
  }

  final <T>T newResourceInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
    final T instance = newInstanceSansFields(clazz, true);
    if (instance != null)
      injectFields(instance);

    return instance;
  }

  private static final Object[] NULL = {};
  private static final Predicate<Field> injectableFieldPredicate = field -> {
    final int modifiers = field.getModifiers();
    return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers);
  };
  private Map<Class<?>,Object[]> contextInsances;

  @SuppressWarnings("unchecked")
  final <T>T getProviderInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
    if (contextInsances == null) {
      contextInsances = new HashMap<>();
    }
    else {
      final Object[] instanceUninjectedFields = contextInsances.get(clazz);
      if (instanceUninjectedFields != null) {
        if (instanceUninjectedFields == NULL)
          return null;

        final Object instance = instanceUninjectedFields[0];
        final Field[] uninjectedFields = (Field[])instanceUninjectedFields[1];
        if (uninjectedFields != null)
          instanceUninjectedFields[1] = injectFields(instance, uninjectedFields);

        return (T)instance;
      }
    }

    final T instance = newInstanceSansFields(clazz, false);
    if (instance != null) {
      final Field[] uninjectedFields = injectFields(instance, Classes.getDeclaredFieldsDeep(instance.getClass(), injectableFieldPredicate));
      contextInsances.put(clazz, new Object[] {instance, uninjectedFields});
    }
    else {
      contextInsances.put(clazz, NULL);
    }

    return instance;
  }

  private <T>T newInstanceSansFields(final Class<T> clazz, final boolean isResource) throws IllegalAccessException, InstantiationException, InvocationTargetException, IOException {
    final Constructor<T>[] constructors = getConstructors(clazz);

    OUT:
    for (final Constructor<T> constructor : constructors) { // [A]
      if (constructor.getParameterCount() == 0)
        return constructor.newInstance();

      final Parameter[] parameters = constructor.getParameters();
      final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
      final Object[] arguments = new Object[parameters.length];
      for (int i = 0; i < parameters.length; ++i) { // [A]
        final Parameter parameter = parameters[i];
        final Annotation[] annotations = parameterAnnotations[i];
        final Annotation injectableAnnotation = findInjectableAnnotation(annotations, isResource);
        if (injectableAnnotation == null) {
          logger.warn("Unsupported parameter type: " + parameter.getName() + " on: " + clazz.getName() + "(" + Arrays.stream(parameters).map(p -> p.getType().getSimpleName()).collect(Collectors.joining(",")) + ")");
          continue OUT;
        }

        arguments[i] = findInjectableValueFromCache(parameter, annotations, parameter.getType(), parameter.getParameterizedType());
      }

      return constructor.newInstance(arguments);
    }

    throw new InstantiationException("No suitable constructor found on provider " + clazz.getName());
  }

  private Field[] injectFields(final Object instance, final Field[] fields) throws IllegalAccessException, IOException {
    return injectFields(instance, fields, 0, 0);
  }

  private Field[] injectFields(final Object instance, final Field[] fields, final int index, final int depth) throws IllegalAccessException, IOException {
    if (index == fields.length)
      return depth == 0 ? null : new Field[depth];

    final Field field = fields[index];
    final Object value = findInjectableValue(field, field.getAnnotations(), field.getType(), field.getGenericType());
    if (value == null) {
      final Field[] uninjectedFields = injectFields(instance, fields, index + 1, depth + 1);
      uninjectedFields[depth] = field;
      return uninjectedFields;
    }

    field.setAccessible(true);
    field.set(instance, value);
    return injectFields(instance, fields, index + 1, depth);
  }

  /**
   * Duplicate version of {@link #injectFields(Object,Field[])} that does not return an array.
   *
   * @param instance The instance of which its fields should be injected.
   * @throws IllegalAccessException If an illegal access error has occurred.
   * @throws IOException If an I/O error has occurred.
   */
  private void injectFields(final Object instance) throws IllegalAccessException, IOException {
    Classes.getDeclaredFieldsDeep(instance.getClass(), Throwing.rethrow((ThrowingPredicate<Field,?>)(final Field field) -> {
      final int modifiers = field.getModifiers();
      if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
        final Object value = findInjectableValueFromCache(field, field.getAnnotations(), field.getType(), field.getGenericType());
        if (value != null) {
          field.setAccessible(true);
          field.set(instance, value);
        }
      }

      return false;
    }));
  }
}