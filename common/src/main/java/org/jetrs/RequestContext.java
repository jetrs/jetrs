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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
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
 * @param <R> The type parameter of the {@link RuntimeContext}.
 * @param <P> The type parameter of the {@link PropertiesAdapter}.
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0
 *      [9.2]</a>
 */
abstract class RequestContext<R extends RuntimeContext,P> extends InterceptorContextImpl<P> {
  private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);
  private static final Comparator<Constructor<?>> parameterCountComparator = Comparator.comparingInt((final Constructor<?> c) -> -c.getParameterCount());
  @SuppressWarnings("rawtypes")
  private static final HashMap<Class<?>,Constructor[]> classToConstructors = new HashMap<>();

  // Gets the constructor with most args [JAX-RS 2.1 3.1.2]
  @SuppressWarnings("unchecked")
  private static <T> Constructor<T>[] getConstructors(final Class<T> clazz) {
    Constructor<T>[] constructors = classToConstructors.get(clazz);
    if (constructors == null)
      classToConstructors.put(clazz, constructors = (Constructor<T>[])clazz.getConstructors());

    Arrays.sort(constructors, parameterCountComparator);
    return constructors;
  }

  final R runtimeContext;
  private Components components;
  private Request request;
  ProvidersImpl providers;

  RequestContext(final PropertiesAdapter<P> propertiesAdapter, final R runtimeContext, final Request request) {
    super(propertiesAdapter);
    this.method = request.getMethod();
    this.runtimeContext = runtimeContext;
    this.components = runtimeContext.getComponents();
    this.request = request;
    this.providers = new ProvidersImpl(this);
  }

  public final Request getRequest() {
    return request;
  }

  @SuppressWarnings("static-method")
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

  final ComponentSet<MessageBodyComponent<ReaderInterceptor>> getReaderInterceptorComponents() {
    if (readerInterceptorCalled)
      throw new IllegalStateException();

    readerInterceptorCalled = true;
    return components.getReaderInterceptorComponents();
  }

  private boolean writerInterceptorCalled = false;

  final ComponentSet<MessageBodyComponent<WriterInterceptor>> getWriterInterceptorComponents() {
    if (writerInterceptorCalled)
      throw new IllegalStateException();

    writerInterceptorCalled = true;
    return components.getWriterInterceptorComponents();
  }

  final ComponentSet<MessageBodyComponent<MessageBodyReader<?>>> getMessageBodyReaderComponents() {
    return components.getMessageBodyReaderComponents();
  }

  final ComponentSet<MessageBodyComponent<MessageBodyWriter<?>>> getMessageBodyWriterComponents() {
    return components.getMessageBodyWriterComponents();
  }

  final ComponentSet<TypeComponent<ExceptionMapper<?>>> getExceptionMapperComponents() {
    return components.getExceptionMapperComponents();
  }

  /**
   * Return the injectable annotation from the provided array of {@code annotations}.
   *
   * @param annotations The annotations in which to find and return the injectable annotation.
   * @param isFromRootResource Whether the array of annotations is from a root resource.
   * @return The injectable annotation from the provided array of {@code annotations}.
   */
  Annotation findInjectableAnnotation(final Annotation[] annotations, final boolean isFromRootResource) {
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType() == Context.class)
        return annotation;

    return null;
  }

  static final Object CONTEXT_NOT_FOUND = new Object();

  @SuppressWarnings("unchecked")
  <T> T findInjectableContextValue(final Class<T> clazz) {
    if (Request.class.isAssignableFrom(clazz))
      return (T)request;

    if (Providers.class.isAssignableFrom(clazz))
      return (T)providers;

    return (T)CONTEXT_NOT_FOUND;
  }

  private static List<Object> makeCacheKey(final Annotation[] annotations, final Class<?> rawType, final Type genericType) {
    final Object[] key = new Object[annotations.length + 2];
    int i = 0;
    while (i < annotations.length)
      key[i] = annotations[i++];

    key[i] = rawType;
    key[++i] = genericType;
    return Arrays.asList(key);
  }

  private Map<List<Object>,Object> injectedValueCache;

  @SuppressWarnings("unchecked")
  <T> T findInjectableValueFromCache(final AnnotatedElement element, final int parameterIndex, final Annotation[] annotations, final Class<T> rawType, final Type genericType) throws IOException {
    if (injectedValueCache == null) {
      injectedValueCache = new HashMap<>();
    }
    else {
      final List<Object> key = makeCacheKey(annotations, rawType, genericType);
      final Object instance = injectedValueCache.get(key);
      if (instance != null)
        return (T)instance;
    }

    final T instance = findInjectableValue(element, parameterIndex, annotations, rawType, genericType);
    if (instance == null)
      return null;

    final List<Object> key = makeCacheKey(annotations, rawType, genericType);
    injectedValueCache.put(key, instance);
    return instance;
  }

  /**
   * Returns the injectable value in the given {@code element} with the provided parameters.
   *
   * @param <T> The type parameter of the injectable value.
   * @param element The {@link AnnotatedElement} in which to find the injectable value.
   * @param parameterIndex The index of the parameter for which to find the injectable value.
   * @param annotations The annotations provided on the {@code element}.
   * @param rawType The class of the injectable value to return.
   * @param genericType The generic type of the injectable value to return.
   * @return The injectable value in the given {@code element} with the provided parameters.
   * @throws IOException If an I/O error has occurred.
   */
  <T> T findInjectableValue(final AnnotatedElement element, final int parameterIndex, final Annotation[] annotations, final Class<T> rawType, final Type genericType) throws IOException {
    return findInjectableContextValue(rawType);
  }

  final <T> T newResourceInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
    final T instance = newInstanceSansFields(clazz, true);
    if (instance != null) {
      injectFields(instance);
      postConstruct(instance);
    }

    return instance;
  }

  private static final Object[] NULL = {};
  private Map<Class<?>,Object[]> contextInsances;

  @SuppressWarnings("unchecked")
  final <T> T getProviderInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
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
      final Field[] fields = Classes.getDeclaredFieldsDeep(clazz, Component::isFieldInjectable);
      final Field[] uninjectedFields = injectFields(instance, fields);
      contextInsances.put(clazz, new Object[] {instance, uninjectedFields});
      postConstruct(instance);
    }
    else {
      contextInsances.put(clazz, NULL);
    }

    return instance;
  }

  private <T> T newInstanceSansFields(final Class<T> clazz, final boolean isResource) throws IllegalAccessException, InstantiationException, InvocationTargetException, IOException {
    final Constructor<T>[] constructors = getConstructors(clazz);

    OUT:
    for (final Constructor<T> constructor : constructors) { // [A]
      if (constructor.getParameterCount() == 0)
        return constructor.newInstance();

      final Parameter[] parameters = constructor.getParameters();
      final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
      final Object[] arguments = new Object[parameters.length];
      for (int i = 0, i$ = parameters.length; i < i$; ++i) { // [A]
        final Parameter parameter = parameters[i];
        final Annotation[] annotations = parameterAnnotations[i];
        final Annotation annotation = findInjectableAnnotation(annotations, isResource);
        if (annotation == null) {
          if (logger.isWarnEnabled()) { logger.warn("Unsupported parameter " + parameter.getName() + " on " + constructor); }
          continue OUT;
        }

        arguments[i] = findInjectableValueFromCache(parameter, -1, annotations, parameter.getType(), parameter.getParameterizedType());
      }

      return constructor.newInstance(arguments);
    }

    throw new InstantiationException("No suitable constructor found on provider " + clazz.getName());
  }

  private static void postConstruct(final Object instance) throws InstantiationException {
    final Method[] methods = Classes.getDeclaredMethodsWithAnnotation(instance.getClass(), PostConstruct.class);
    if (methods.length == 0)
      return;

    if (methods.length > 1)
      throw new InstantiationException("@PostConstruct annotation specified on multiple methods");

    final Method method = methods[0];
    if (method.getParameterCount() > 0)
      throw new InstantiationException("@PostConstruct method must have no parameters");

    try {
      method.setAccessible(true);
      method.invoke(instance);
    }
    catch (final Exception e) {
      final InstantiationException ie = new InstantiationException();
      ie.initCause(e.getCause());
      throw ie;
    }
  }

  private Field[] injectFields(final Object instance, final Field[] fields) throws IllegalAccessException, IOException {
    return injectFields(instance, fields, fields.length, 0, 0);
  }

  private Field[] injectFields(final Object instance, final Field[] fields, final int length, final int index, final int depth) throws IllegalAccessException, IOException {
    if (index == length)
      return depth == 0 ? null : new Field[depth];

    final Field field = fields[index];
    final Object value = findInjectableValueFromCache(field, -1, Classes.getAnnotations(field), field.getType(), field.getGenericType());
    if (value == null) {
      final Field[] uninjectedFields = injectFields(instance, fields, length, index + 1, depth + 1);
      uninjectedFields[depth] = field;
      return uninjectedFields;
    }

    field.setAccessible(true);
    field.set(instance, value);
    return injectFields(instance, fields, length, index + 1, depth);
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
        final Object value = findInjectableValueFromCache(field, -1, Classes.getAnnotations(field), field.getType(), field.getGenericType());
        if (value != null) {
          field.setAccessible(true);
          field.set(instance, value);
        }
      }

      return false;
    }));
  }

  @Override
  public void close() throws IOException {
    super.close();

    components = null;
    request = null;
    providers = null;
  }
}