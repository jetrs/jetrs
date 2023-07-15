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

import static org.jetrs.HttpHeaders.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.libj.lang.Classes;
import org.libj.lang.Numbers;
import org.libj.lang.Throwables;
import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerRequestContextImpl extends RequestContext<ServerRuntimeContext,HttpServletRequest> implements Closeable, ContainerRequestContext, ReaderInterceptorContext {
  private static final Logger logger = LoggerFactory.getLogger(ContainerRequestContextImpl.class);

  enum Stage {
    REQUEST_FILTER_PRE_MATCH,
    REQUEST_MATCH,
    REQUEST_FILTER,
    SERVICE,
    RESPONSE_FILTER,
    RESPONSE_WRITE
  }

  @SuppressWarnings("unchecked")
  static final Class<Annotation>[] injectableAnnotationTypes = new Class[] {CookieParam.class, FormParam.class, HeaderParam.class, MatrixParam.class, PathParam.class, QueryParam.class};
  // FIXME: Support `AsyncResponse` (JAX-RS 2.1 8.2)

  private static Field[] EMPTY_FIELDS = {};

  static final Field[] getContextFields(final Class<?> cls) {
    final Field[] fields = Classes.getDeclaredFieldsDeep(cls);
    return getContextFields(fields, fields.length, 0, 0);
  }

  private static Field[] getContextFields(final Field[] fields, final int length, final int index, final int depth) {
    if (index == length)
      return depth == 0 ? EMPTY_FIELDS : new Field[depth];

    final Field field = fields[index];
    boolean hasContext = false;
    for (int i = 0, i$ = injectableAnnotationTypes.length; i < i$; ++i) // [A]
      if (hasContext |= field.isAnnotationPresent(injectableAnnotationTypes[i]))
        break;

    hasContext |= field.isAnnotationPresent(Context.class);

    if (!hasContext)
      return getContextFields(fields, length, index + 1, depth);

    final Field[] result = getContextFields(fields, length, index + 1, depth + 1);
    result[depth] = field;
    return result;
  }

  private final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;

  private HttpServletRequest httpServletRequest;
  private HttpServletResponse httpServletResponse;
  private ContainerResponseContextImpl containerResponseContext;

  private ArrayList<ResourceInfoImpl> resourceInfos;

  private ResourceMatches resourceMatches;
  private ResourceMatch resourceMatch;
  private ResourceInfoImpl resourceInfo;
  private UriInfoImpl uriInfo;

  private HttpHeadersImpl headers;

  ContainerRequestContextImpl(final PropertiesAdapter<HttpServletRequest> propertiesAdapter, final ServerRuntimeContext runtimeContext, final Request request) {
    super(propertiesAdapter, runtimeContext, request);
    this.readerInterceptorProviderFactories = getReaderInterceptorFactoryList();
  }

  void init(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
    this.resourceInfos = runtimeContext.getResourceInfos();
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = new ContainerResponseContextImpl(propertiesAdapter, httpServletRequest, httpServletResponse, this);
    this.uriInfo = new UriInfoImpl(httpServletRequest, this);
    this.headers = new HttpHeadersImpl(httpServletRequest);
  }

  private Stage stage;

  Stage getStage() {
    return this.stage;
  }

  void setStage(final Stage stage) {
    this.stage = stage;
  }

  @Override
  HttpServletRequest getProperties() {
    return httpServletRequest;
  }

  @Override
  HttpHeadersImpl getHttpHeaders() {
    return headers;
  }

  @Override
  public final UriInfoImpl getUriInfo() {
    return uriInfo;
  }

  ResourceMatches getResourceMatches() {
    return resourceMatches;
  }

  ResourceMatch getResourceMatch() {
    return resourceMatch;
  }

  ContainerResponseContextImpl getContainerResponseContext() {
    return containerResponseContext;
  }

  private boolean paramConverterProviderCalled = false;

  ArrayList<ProviderFactory<ParamConverterProvider>> getParamConverterProviderFactoryList() {
    if (paramConverterProviderCalled)
      throw new IllegalStateException();

    paramConverterProviderCalled = true;
    return runtimeContext.getParamConverterProviderFactories();
  }

  private boolean preMatchRequestFilterCalled = false;

  void filterPreMatchContainerRequest() throws IOException {
    if (preMatchRequestFilterCalled)
      throw new IllegalStateException();

    preMatchRequestFilterCalled = true;
    final ArrayList<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories = runtimeContext.getPreMatchContainerRequestFilterProviderFactories();
    final int i$ = preMatchContainerRequestFilterProviderFactories.size();
    for (int i = 0; i < i$; ++i) // [RA]
      preMatchContainerRequestFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(this);
  }

  private boolean requestFilterCalled = false;

  void filterContainerRequest() throws IOException {
    if (requestFilterCalled)
      throw new IllegalStateException();

    requestFilterCalled = true;
    final ArrayList<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories = runtimeContext.getContainerRequestFilterProviderFactories();
    for (int i = 0, i$ = containerRequestFilterProviderFactories.size(); i < i$; ++i) // [RA]
      containerRequestFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(this);
  }

  void filterContainerResponse() throws IOException {
    final ArrayList<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories = runtimeContext.getContainerResponseFilterProviderFactories();
    for (int i = 0, i$ = containerResponseFilterProviderFactories.size(); i < i$; ++i) // [RA]
      containerResponseFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(this, containerResponseContext);
  }

  @Override
  Annotation findInjectableAnnotation(final Annotation[] annotations, final boolean isResource) {
    final Annotation injectableAnnotation = super.findInjectableAnnotation(annotations, isResource);
    if (injectableAnnotation != null || !isResource)
      return injectableAnnotation;

    for (final Annotation annotation : annotations) // [A]
      for (final Class<Annotation> injectableAnnotationType : injectableAnnotationTypes) // [A]
        if (annotation.annotationType() == injectableAnnotationType)
          return annotation;

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T>T findInjectableContextValue(final Class<T> clazz) {
    // FIXME: Support ResourceContext
    // if (ResourceContext.class.isAssignableFrom(clazz))

    // FIXME: Support ResourceContext (JAX-RS 2.1 6.5.1)
    // if (ResourceInfo.class.isAssignableFrom(clazz))

    if (Configuration.class.isAssignableFrom(clazz))
      return (T)runtimeContext.getConfiguration();

    if (Application.class.isAssignableFrom(clazz))
      return (T)runtimeContext.getApplication();

    if (ServletConfig.class.isAssignableFrom(clazz))
      return (T)runtimeContext.getServletConfig();

    if (ServletContext.class.isAssignableFrom(clazz))
      return (T)runtimeContext.getServletContext();

    if (HttpHeaders.class.isAssignableFrom(clazz))
      return (T)getHeaders();

    if (HttpServletRequest.class.isAssignableFrom(clazz))
      return (T)httpServletRequest;

    if (HttpServletResponse.class.isAssignableFrom(clazz))
      return (T)httpServletResponse;

    if (ResourceInfo.class.isAssignableFrom(clazz))
      return (T)resourceInfo;

    if (UriInfo.class.isAssignableFrom(clazz))
      return (T)getUriInfo();

    if (SecurityContext.class.isAssignableFrom(clazz))
      return (T)getSecurityContext();

    if (ContainerRequestContext.class.isAssignableFrom(clazz))
      return (T)this;

    if (ContainerResponseContext.class.isAssignableFrom(clazz))
      return (T)containerResponseContext;

    return super.findInjectableContextValue(clazz);
  }

  final Object invokeMethod(final Object obj) throws IllegalAccessException, InvocationTargetException, IOException {
    final Method resourceMethod = resourceInfo.getResourceMethod();
    if (resourceInfo.getParameterCount() == 0)
      return resourceMethod.invoke(obj);

    final Parameter[] parameters = resourceInfo.getMethodParameters();
    final Class<?>[] parameterTypes = resourceInfo.getMethodParameterTypes();
    final Type[] genericParameterTypes = resourceInfo.getMethodGenericParameterTypes();
    final Annotation[][] parameterAnnotations = resourceInfo.getMethodParameterAnnotations();
    final Object[] arguments = new Object[parameters.length];
    for (int i = 0, i$ = parameters.length; i < i$; ++i) { // [A]
      final Object arg = arguments[i] = findInjectableValueFromCache(parameters[i], i, parameterAnnotations[i], parameterTypes[i], genericParameterTypes[i]);
      if (arg instanceof Exception)
        throw new BadRequestException((Exception)arg);
    }

    return resourceMethod.invoke(obj, arguments);
  }

  @Override
  @SuppressWarnings("unchecked")
  <T>T findInjectableValue(final AnnotatedElement element, final int parameterIndex, final Annotation[] annotations, final Class<T> rawType, final Type genericType) throws IOException {
    T injectableObject = super.findInjectableValue(element, parameterIndex, annotations, rawType, genericType);
    if (injectableObject != null)
      return injectableObject;

    final Annotation annotation = findInjectableAnnotation(annotations, true);
    if (annotation != null) {
      final Object argument = getParamObject(element, parameterIndex, annotation, annotations, rawType, genericType);
      if (argument instanceof Exception)
        throw new BadRequestException((Exception)argument);

      return (T)argument;
    }

    // The rest of this concerns the message body, and thus only applies to method parameters
    if (!(element instanceof Parameter))
      return null;

    final Providers providers = getProviders();
    MediaType contentType = getMediaType();
    if (contentType == null)
      contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

    final MessageBodyReader<?> messageBodyReader = providers.getMessageBodyReader(rawType, genericType, annotations, contentType); // [JAX-RS 4.2.1]
    if (messageBodyReader == null)
      throw new WebApplicationException("Could not find MessageBodyReader for {type=" + rawType.getName() + ", genericType=" + genericType.getTypeName() + ", annotations=" + Arrays.toString(annotations) + ", mediaType=" + contentType + "}");

    // FIXME: Why is there a return type for ReaderInterceptorContext#proceed()? And it's of type Object. What type is ReaderInterceptorContext supposed to return? It should be InputStream, but then it makes it redundant.
    setType(rawType);
    setGenericType(rawType.getGenericSuperclass());
    setAnnotations(annotations);
    try {
      return (T)readBody(messageBodyReader);
    }
    catch (final NoContentException e) {
      throw new BadRequestException(e); // [JAX-RS 4.2.4]
    }
  }

  private DefaultValueImpl getDefaultValue(final AnnotatedElement element, final int parameterIndex) {
    return resourceInfo.getDefaultValue(element, parameterIndex);
  }

  private static boolean componentTypeMatches(final Class<?> componentType, final Object value) {
    return componentType.isInstance(value);
  }

  private static List<?> getHeaderValues(final Class<?> componentType, final MirrorQualityList<String,Object> headerStringValues, final String headerName) {
    if (componentType == String.class)
      return headerStringValues;

    final MirrorQualityList<Object,String> headerObjectValue = headerStringValues.getMirrorList();
    final Object headerValue = headerObjectValue.get(0);
    if (headerValue != null && componentTypeMatches(componentType, headerValue))
      return headerObjectValue;

    throw new BadRequestException("Invalid header value: " + headerName + ": " + headerStringValues.get(0)); // [JAX-RS 3.2]
  }

  @SuppressWarnings("unchecked")
  private Object getParamObject(final AnnotatedElement element, final int parameterIndex, final Annotation annotation, final Annotation[] annotations, final Class<?> rawType, final Type genericType) throws IOException {
    final Class<? extends Annotation> annotationType = annotation.annotationType();
    if (annotationType == CookieParam.class) {
      final Map<String,Cookie> cookies = getCookies();
      final Object value;
      if (cookies != null && (value = cookies.get(((CookieParam)annotation).value())) != null)
        return value; // FIXME: This is wrong!

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);

      final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
      if (defaultValue == null)
        return paramPlurality.getNullValue(rawType);

      if (defaultValue.isConverted)
        return defaultValue.convertedValue;

      final String firstValue = defaultValue.annotatedValue;

      // FIXME: Param types other than `Cookie` still need to be implemented.
      return DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, firstValue, null, false, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotationType == HeaderParam.class) {
      final String headerName = ((HeaderParam)annotation).value();
      String firstValue = getHttpHeaders().getString(headerName);

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);
      if (firstValue == null) {
        final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
        if (defaultValue == null)
          return paramPlurality.getNullValue(rawType);

        if (defaultValue.isConverted)
          return defaultValue.convertedValue;

        firstValue = defaultValue.annotatedValue;
      }

      final MirrorQualityList<String,Object> headerStringValues = getHttpHeaders().get(headerName);
      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Class<?> componentType = paramPlurality.getMemberClass(annotationType, genericType);
        final List<?> headerValues = getHeaderValues(componentType, headerStringValues, headerName);
        // FIXME: Note this does not consider the generic type of the list -- should it try to do a conversion if the classes don't match?!
        if (rawType.isAssignableFrom(MirrorQualityList.class))
          return headerValues;

        @SuppressWarnings("rawtypes")
        final Collection list = ParamPlurality.COLLECTION.newContainer(rawType, parameterIndex);
        list.addAll(headerValues);
        return list;
      }

      if (paramPlurality == ParamPlurality.ARRAY) {
        // FIXME: Note this does not consider the generic type of the list -- should it try to do a conversion if the classes don't match?!
        final Class<?> componentType = paramPlurality.getMemberClass(annotationType, genericType);
        final List<?> headerValues = getHeaderValues(componentType, headerStringValues, headerName);
        return headerValues.toArray((Object[])Array.newInstance(componentType, headerValues.size()));
      }

      if (rawType == String.class)
        return headerStringValues.get(0);

      final MirrorQualityList<Object,String> headerObjectValue = headerStringValues.getMirrorList();
      final Object obj = headerObjectValue.get(0);
      if (rawType.isInstance(obj))
        return obj;

      final Object converted = DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, firstValue, null, false, runtimeContext.getParamConverterProviderFactories(), this);
      if (converted != null)
        return converted;

      throw new BadRequestException("Invalid header value: " + headerName + ": " + headerStringValues.get(0)); // [JAX-RS 3.2]
    }

    if (annotationType == FormParam.class) {
      final InputStream entityStream = getEntityStream();
      String firstValue = null;
      List<String> values = null;
      if (entityStream != null) {
        final MultivaluedArrayMap<String,String> map = EntityUtil.readFormParams(entityStream, MediaTypes.getCharset(getMediaType()), EntityUtil.shouldDecode(annotations));
        values = map.get(((FormParam)annotation).value());
      }

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);
      if (values == null) {
        final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
        if (defaultValue == null)
          return paramPlurality.getNullValue(rawType);

        if (defaultValue.isConverted)
          return defaultValue.convertedValue;

        firstValue = defaultValue.annotatedValue;
      }

      return DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, firstValue, values, false, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotationType == QueryParam.class) {
      final boolean decode = EntityUtil.shouldDecode(annotations);
      List<String> values = getUriInfo().getQueryParameters(decode).get(((QueryParam)annotation).value());
      String firstValue = null;

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);
      if (values == null) {
        final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
        if (defaultValue == null)
          return paramPlurality.getNullValue(rawType);

        if (defaultValue.isConverted)
          return defaultValue.convertedValue;

        firstValue = defaultValue.annotatedValue;
      }

      return DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, firstValue, values, false, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotationType == PathParam.class) {
      final boolean decode = EntityUtil.shouldDecode(annotations);
      final String pathParamNameToMatch = ((PathParam)annotation).value();

      if (rawType == PathSegment.class) {
        final ArrayList<PathSegment> pathSegments = getUriInfo().getPathSegments(decode);

        final String[] pathParamNames = resourceMatch.getPathParamNames();
        final long[] regionStartEnds = resourceMatch.getRegionStartEnds();
        for (int p = 0, segStart = 0, segEnd; p < pathParamNames.length; ++p) { // [A]
          if (matches(pathParamNameToMatch, pathParamNames[p])) {
            final long regionStartEnd = regionStartEnds[p];
            final int regionStart = Numbers.Composite.decodeInt(regionStartEnd, 0);
            final int regionEnd = Numbers.Composite.decodeInt(regionStartEnd, 1);

            for (int i = 0, i$ = pathSegments.size(); i < i$; ++i) { // [RA]
              final PathSegment pathSegment = pathSegments.get(i);
              final String path = ((PathSegmentImpl)pathSegment).getPathEncoded();
              segEnd = segStart + path.length();
              if (rangeIntersects(segStart, segEnd, regionStart, regionEnd))
                return pathSegment;

              segStart = segEnd + 1; // add '/' char
            }
          }
        }

        if (logger.isWarnEnabled()) if (logger.isWarnEnabled()) logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") PathSegment not found in URI template of @Path on: " + element);
        return null;
      }

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);

      final Class<?> memberClass;
      if ((paramPlurality == ParamPlurality.COLLECTION || paramPlurality == ParamPlurality.ARRAY) && (memberClass = paramPlurality.getMemberClass(rawType, genericType)) == PathSegment.class) {
        final ArrayList<PathSegment> pathSegments = getUriInfo().getPathSegments(decode);

        int segStart = 0, segEnd;
        final String[] pathParamNames = resourceMatch.getPathParamNames();
        final long[] regionStartEnds = resourceMatch.getRegionStartEnds();
        final Collection<PathSegment> matchedSegments = paramPlurality == ParamPlurality.ARRAY ? new ArrayList<>() : (Collection<PathSegment>)paramPlurality.newContainer(rawType, Integer.MAX_VALUE); // FIXME: Size is unknown at this time.
        OUT:
        for (int i = 0, j = 0; i < pathParamNames.length; ++i) { // [A]
          if (matches(pathParamNameToMatch, pathParamNames[i])) {
            boolean inRegion = false;
            final long regionStartEnd = regionStartEnds[i];
            final int regionStart = Numbers.Composite.decodeInt(regionStartEnd, 0);
            final int regionEnd = Numbers.Composite.decodeInt(regionStartEnd, 1);
            while (true) {
              final PathSegment pathSegment = pathSegments.get(j);
              final String path = ((PathSegmentImpl)pathSegment).getPathEncoded();
              segEnd = segStart + path.length();

              if (inRegion) {
                if (rangeIntersects(segStart, segEnd, regionStart, regionEnd))
                  matchedSegments.add(pathSegment);
                else
                  break;
              }
              else if (inRegion = rangeIntersects(segStart, segEnd, regionStart, regionEnd)) {
                matchedSegments.add(pathSegment);
              }

              if (++j == pathSegments.size())
                break OUT;

              segStart = segEnd + 1; // add '/' char
            }
          }
        }

        return paramPlurality == ParamPlurality.ARRAY ? matchedSegments.toArray((Object[])Array.newInstance(memberClass, matchedSegments.size())) : matchedSegments;
      }

      final MultivaluedArrayMap<String,String> pathParameters = getUriInfo().getPathParameters(decode);
      List<String> values = pathParameters.get(pathParamNameToMatch);
      // FIXME: Another useful warning would be: notify if more than 1 @PathParam annotations specify the same name
      String value = null;
      if (values == null) {
        if (logger.isWarnEnabled()) logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") not found in URI template of @Path on: " + element);

        final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
        if (defaultValue != null) {
          if (defaultValue.isConverted)
            return defaultValue.convertedValue;

          value = defaultValue.annotatedValue;
        }
      }

      return DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, value, values, false, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotationType == MatrixParam.class) {
      final boolean decode = EntityUtil.shouldDecode(annotations);
      final ArrayList<PathSegmentImpl> pathSegments = getUriInfo().getPathSegmentImpls(decode);

      final int pathSegremntsSize = pathSegments.size();
      final List<String> values;
      if (pathSegremntsSize == 0) {
        values = null;
      }
      else {
        final String[] matrixParams = getMatrixParamValue(((MatrixParam)annotation).value(), pathSegments, pathSegremntsSize);
        values = matrixParams == null ? null : Arrays.asList(matrixParams);
      }

      final ParamPlurality<?> paramPlurality = ParamPlurality.fromClass(rawType);
      final String firstValue;
      if (values == null) {
        final DefaultValueImpl defaultValue = getDefaultValue(element, parameterIndex);
        if (defaultValue == null)
          return paramPlurality.getNullValue(rawType);

        if (defaultValue.isConverted)
          return defaultValue.convertedValue;

        firstValue = defaultValue.annotatedValue;
      }
      else {
        firstValue = values.get(0);
      }

      return DefaultParamConverterProvider.convertParameter(rawType, genericType, annotations, paramPlurality, firstValue, values, false, runtimeContext.getParamConverterProviderFactories(), this);
    }

    throw new UnsupportedOperationException("Unsupported param annotation type: " + annotationType);
  }

  private String[] getMatrixParamValue(final String matrixParamName, final ArrayList<PathSegmentImpl> pathSegments, final int size) {
    for (int i = 0; i < size; ++i) { // [RA]
      final MultivaluedMap<String,String> matrixParameters = pathSegments.get(i).getMatrixParameters();
      if (matrixParameters.size() > 0)
        return getMatrixParamValues(matrixParamName, pathSegments, i, size, matrixParameters.entrySet().iterator(), 0);
    }

    return null;
  }

  private String[] getMatrixParamValues(final String matrixParamName, final List<PathSegmentImpl> pathSegments, int index, final int size, final Iterator<Map.Entry<String,List<String>>> iterator, final int depth) {
    if (!iterator.hasNext()) {
      for (int i = ++index; i < size; ++i) { // [RA]
        final MultivaluedMap<String,String> matrixParameters = pathSegments.get(i).getMatrixParameters();
        if (matrixParameters.size() > 0)
          return getMatrixParamValues(matrixParamName, pathSegments, i, size, matrixParameters.entrySet().iterator(), depth);
      }

      return depth == 0 ? null : new String[depth];
    }

    final Map.Entry<String,List<String>> entry = iterator.next();
    if (!matrixParamName.equals(entry.getKey()))
      return getMatrixParamValues(matrixParamName, pathSegments, index, size, iterator, depth);

    final List<String> values = entry.getValue();
    final int i$ = values.size();
    final String[] array = getMatrixParamValues(matrixParamName, pathSegments, index, size, iterator, depth + i$);
    for (int i = 0; i < i$; ++i) // [RA]
      array[depth + i] = values.get(i);

    return array;
  }

  private static boolean rangeIntersects(final int startA, final int endA, final int startB, final int endB) {
    return startA <= endB && startB <= endA;
  }

  private static boolean matches(final String pathParamNameToMatch, final String pathParamName) {
    return pathParamName.startsWith(pathParamNameToMatch + UriTemplate.DEL) && pathParamName.lastIndexOf(UriTemplate.DEL, pathParamName.length() - 1) == pathParamNameToMatch.length();
  }

  boolean filterAndMatch() {
    final ResourceMatches resourceMatches = filterAndMatch(httpServletRequest.getMethod(), false);
    if (resourceMatches == null)
      return false;

    if (logger.isWarnEnabled() && resourceMatches.size() > 1 && resourceMatches.get(0).compareTo(resourceMatches.get(1)) == 0) {
      final StringBuilder builder = new StringBuilder("Multiple resources match ambiguously for request to \"" + httpServletRequest.getRequestURI() + "\": {");
      for (int i = 0, i$ = resourceMatches.size(); i < i$; ++i) // [RA]
        builder.append('"').append(resourceMatches.get(i)).append("\", ");

      builder.setCharAt(builder.length() - 1, '}');
      logger.warn(builder.toString());
    }

    // FIXME: Note that this code always picks the 1st ResourceMatch.
    // FIXME: This is done under the assumption that it is not possible to have a situation where
    // FIXME: any other ResourceMatch would be retrieved. Is this truly the case?!
    this.resourceMatches = resourceMatches;
    this.resourceMatch = resourceMatches.get(0);
    this.resourceInfo = resourceMatch.getResourceInfo();
    return true;
  }

  private static int[] normalizeUri(final StringBuilder requestUriBuilder, final String path, final int len, final boolean inMatrix, final int start, final int depth) {
    for (int i = start; i < len; ++i) { // [N]
      final char ch = path.charAt(i);
      if (inMatrix) {
        if (ch != '/')
          continue;

        requestUriBuilder.append(ch);
        final int[] ret = normalizeUri(requestUriBuilder, path, len, false, i + 1, depth + 1);
//        ret[depth] = i;
        return ret;
      }
      else if (ch == ';') {
        final int[] ret = normalizeUri(requestUriBuilder, path, len, true, i + 1, depth + 1);
//        ret[depth] = i;
        return ret;
      }

      requestUriBuilder.append(ch);
    }

    return null; //new int[depth];
  }

  private ResourceMatches filterAndMatch(final String requestMethod, final boolean isOverride) {
    final UriInfo uriInfo = getUriInfo();

    // Match request URI with matrix params stripped out
    final StringBuilder requestUriBuilder = new StringBuilder(uriInfo.getBaseUri().getRawPath());
    final int baseUriLen = requestUriBuilder.length();

    final String path = uriInfo.getPath();
    normalizeUri(requestUriBuilder, path, path.length(), false, 0, 0);

    final String requestUriMatched = requestUriBuilder.toString();

    TreeSet<String> maybeNotAllowed = null;
    boolean maybeNotSupported = false;
    boolean maybeNotAcceptable = false;
    ResourceMatches resourceMatches = null;
    for (int i = 0, i$ = resourceInfos.size(); i < i$; ++i) { // [RA]
      final ResourceInfoImpl resourceInfo = resourceInfos.get(i);
      final UriTemplate uriTemplate = resourceInfo.getUriTemplate();
      final Matcher matcher = uriTemplate.matcher(requestUriMatched);
      if (!matcher.find())
        continue;

      final HttpMethod httpMethod = resourceInfo.getHttpMethod();
      if (httpMethod == null)
        throw new UnsupportedOperationException("JAX-RS 2.1 3.4.1");

      final String resourceMethod = httpMethod.value();
      if (!requestMethod.equals(resourceMethod)) {
        if (!isOverride) {
          if (maybeNotAllowed == null)
            maybeNotAllowed = new TreeSet<>();

          maybeNotAllowed.add(resourceMethod);
        }

        continue;
      }

      maybeNotSupported = true;
      final MediaType contentType = getMediaType();
      if (!resourceInfo.isCompatibleContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_TYPE)) // [JAX-RS 4.2.1]
        continue;

      maybeNotAcceptable = true;
      final MediaType[] compatibleMediaTypes = resourceInfo.getCompatibleAccept(getAcceptableMediaTypes(), getHeaders().get(ACCEPT_CHARSET));
      if (compatibleMediaTypes.length == 0)
        continue;

      if (resourceMatches == null)
        resourceMatches = new ResourceMatches();

      final String[] pathParamNames = uriTemplate.getPathParamNames();
      final MultivaluedArrayMap<String,String> pathParameters = new MultivaluedArrayHashMap<>(pathParamNames.length);
      final long[] regionStartEnds = new long[pathParamNames.length];

      for (int j = 0, j$ = pathParamNames.length; j < j$; ++j) { // [A]
        final String pathParamName = pathParamNames[j];
        final String pathParamValue = matcher.group(pathParamName);
        pathParameters.add(pathParamName.substring(0, pathParamName.lastIndexOf(UriTemplate.DEL, pathParamName.length() - 1)), pathParamValue);

        final int start = matcher.start(pathParamName) - baseUriLen;
        final int end = matcher.end(pathParamName) - baseUriLen;
        regionStartEnds[j] = Numbers.Composite.encode(start, end);
      }

      resourceMatches.add(new ResourceMatch(resourceInfo, matcher.group(), compatibleMediaTypes, pathParamNames, regionStartEnds, pathParameters)); // We only care about the highest quality match of the Accept header
    }

    if (resourceMatches != null) {
      resourceMatches.sort(null);
      return resourceMatches;
    }

    if (maybeNotAcceptable)
      throw new NotAcceptableException();

    if (maybeNotSupported)
      throw new NotSupportedException();

    if (maybeNotAllowed == null || isOverride)
      return null;

    if (HttpMethod.OPTIONS.equals(requestMethod)) {
      final Response.ResponseBuilder response = Response.ok();

      for (final String header : maybeNotAllowed) // [S]
        response.header(ACCESS_CONTROL_ALLOW_METHODS, header).header(ALLOW, header);

      final List<String> requestHeaders = getHeaders().get(ACCESS_CONTROL_REQUEST_HEADERS);
      final int i$;
      if (requestHeaders != null && (i$ = requestHeaders.size()) > 0)
        for (int i = 0; i < i$; ++i) // [RA]
          response.header(ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders.get(i));

      final String origin = httpServletRequest.getHeader(ORIGIN);
      if (origin != null) {
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        response.header(VARY, ORIGIN);
      }

      abortWith(response.build());
    }
    else if (HttpMethod.HEAD.equals(requestMethod)) {
      final ResourceMatches matches = filterAndMatch(HttpMethod.GET, true);
      if (matches != null)
        return matches;
    }

    throw new NotAllowedException(Response.status(Response.Status.METHOD_NOT_ALLOWED).allow(maybeNotAllowed).build());
  }

  void service() throws IOException, ServletException {
    final Object result = resourceMatch.service(this);

    if (result instanceof Response) {
      setResponse((Response)result, resourceInfo.getMethodAnnotations());
    }
    else if (result != null) {
      containerResponseContext.setEntity(result);
      containerResponseContext.setAnnotations(resourceInfo.getMethodAnnotations());
      containerResponseContext.setStatusInfo(Response.Status.OK);
    }
    else { // [JAX-RS 3.3.3]
      containerResponseContext.setStatusInfo(Response.Status.NO_CONTENT);
    }
  }

  void setAbortResponse(final AbortFilterChainException e) {
    setResponse(e.getResponse(), null);
  }

  // [JAX-RS 2.1 3.3.4 1]
  @SuppressWarnings({"rawtypes", "unchecked"})
  Response setErrorResponse(final Throwable t) {
    Class cls = t.getClass();
    final Providers providers = getProviders();
    do {
      final ExceptionMapper exceptionMapper = providers.getExceptionMapper(cls);
      if (exceptionMapper != null) {
        final Response response = exceptionMapper.toResponse(t);
        if (response != null)
          return setResponse(response, null);
      }
    }
    while ((cls = cls.getSuperclass()) != null);

    if (t instanceof WebApplicationException)
      return setResponse(((WebApplicationException)t).getResponse(), null);

    return null;
  }

  void sendError(final int scInternalServerError, final Throwable t) throws IOException {
    if (httpServletResponse.isCommitted()) {
      if (logger.isInfoEnabled()) logger.info("Unable to overwrite committed response [" + httpServletResponse.getStatus() + "] -> [" + scInternalServerError + "]: ", t);
    }
    else {
      httpServletResponse.sendError(scInternalServerError, Throwables.toString(t));
    }
  }

  private Response setResponse(final Response response, final Annotation[] annotations) {
    final MultivaluedArrayMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    containerResponseHeaders.clear();

    final MultivaluedMap<String,String> responseHeaders = response.getStringHeaders();
    if (responseHeaders.size() > 0) {
      for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet()) { // [S]
        final List<String> values = entry.getValue();
        final int i$ = values.size();
        if (i$ > 0) {
          final String key = entry.getKey();
          if (values instanceof RandomAccess) {
            int i = 0; do // [RA]
              containerResponseHeaders.add(key, values.get(i));
            while (++i < i$);
          }
          else {
            final Iterator<String> i = values.iterator(); do // [I]
              containerResponseHeaders.add(key, i.next());
            while (i.hasNext());
          }
        }
      }
    }

    // FIXME: Have to hack getting the annotations out of the Response
    final Annotation[] responseAnnotations = ((ResponseImpl)response).annotations;
    final Annotation[] entityAnnotations = annotations == null ? responseAnnotations : responseAnnotations == null ? annotations : ArrayUtil.concat(responseAnnotations, annotations);

    final Object entity = response.hasEntity() ? response.getEntity() : null;
    containerResponseContext.setEntity(entity, entityAnnotations, response.getMediaType());
    containerResponseContext.setStatusInfo(response.getStatusInfo());

    return response;
  }

  void writeResponse(final Throwable t) throws IOException {
    if (httpServletResponse.isCommitted()) {
      if (logger.isInfoEnabled()) logger.info("Unable to overwrite committed response [" + httpServletResponse.getStatus() + "] -> [" + containerResponseContext.getStatus() + "]: ", t);
    }
    else {
      containerResponseContext.writeResponse(httpServletResponse, t != null);
    }
  }

  @Override
  public void setRequestUri(final URI requestUri) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRequestUri(final URI baseUri, final URI requestUri) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedArrayMap<String,String> getHeaders() {
    return getHttpHeaders();
  }

  @Override
  public int getLength() {
    return getHttpHeaders().getLength();
  }

  private Boolean hasEntity;

  private void checkInitEntityStream() {
    if (hasEntity == null) {
      try {
        setEntityStream(httpServletRequest.getInputStream());
      }
      catch (final IOException e) {
        throw new InternalServerErrorException(e);
      }
    }
  }

  @Override
  public boolean hasEntity() {
    checkInitEntityStream();
    return hasEntity;
  }

  private InputStream entityStream;

  @Override
  public InputStream getEntityStream() {
    checkInitEntityStream();
    return entityStream;
  }

  @Override
  public void setEntityStream(InputStream input) {
    if (input == null) {
      entityStream = null;
      hasEntity = false;
    }
    else {
      try {
        entityStream = EntityUtil.makeConsumableNonEmptyOrNull(input, false);
        hasEntity = entityStream != null;
      }
      catch (final IOException e) {
        throw new InternalServerErrorException(e);
      }
    }
  }

  private SecurityContext securityContext;

  @Override
  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  @Override
  public void setSecurityContext(final SecurityContext context) {
    if (getStage().ordinal() >= Stage.RESPONSE_FILTER.ordinal())
      throw new IllegalStateException();

    this.securityContext = context;
  }

  @Override
  public InputStream getInputStream() {
    return getEntityStream();
  }

  @Override
  public void setInputStream(final InputStream is) {
    setEntityStream(is);
  }

  private Object lastProceeded;
  private int interceptorIndex = -1;

  @SuppressWarnings("rawtypes")
  private MessageBodyReader messageBodyReader;

  Object readBody(final MessageBodyReader<?> messageBodyReader) throws IOException {
    this.messageBodyReader = messageBodyReader;
    return EntityUtil.checktNotNull(proceed(), getAnnotations());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object proceed() throws IOException, WebApplicationException {
    final int size = readerInterceptorProviderFactories.size();
    if (++interceptorIndex < size)
      return lastProceeded = readerInterceptorProviderFactories.get(interceptorIndex).getSingletonOrFromRequestContext(this).aroundReadFrom(this);

    final InputStream inputStream;
    if (interceptorIndex == size && (inputStream = getInputStream()) != null)
      lastProceeded = messageBodyReader.readFrom(getType(), getGenericType(), getAnnotations(), getMediaType(), getHeaders(), inputStream);

    return lastProceeded;
  }

  @Override
  public void close() throws IOException {
    containerResponseContext.close();
    if (entityStream != null)
      entityStream.close();
  }
}