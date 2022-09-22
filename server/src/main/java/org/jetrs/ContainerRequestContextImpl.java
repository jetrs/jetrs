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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.libj.lang.Classes;
import org.libj.lang.Numbers;
import org.libj.net.BufferedServletInputStream;
import org.libj.util.ArrayUtil;
import org.libj.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerRequestContextImpl extends RequestContext<HttpServletRequest> implements Closeable, ContainerRequestContext, ReaderInterceptorContext {
  private static final Logger logger = LoggerFactory.getLogger(ContainerRequestContextImpl.class);

  enum Stage {
    FILTER_REQUEST_PRE_MATCH,
    MATCH,
    FILTER_REQUEST,
    SERVICE,
    FILTER_RESPONSE,
    WRITE_RESPONSE
  }

  @SuppressWarnings("unchecked")
  static final Class<Annotation>[] injectableAnnotationTypes = new Class[] {CookieParam.class, FormParam.class, HeaderParam.class, MatrixParam.class, PathParam.class, QueryParam.class};
  // FIXME: Support `AsyncResponse` (JAX-RS 2.1 8.2)

  private static Field[] EMPTY_FIELDS = {};

  final static Field[] getContextFields(final Object instance) {
    return getInjectableFields(Classes.getDeclaredFieldsDeep(instance.getClass()), 0, 0);
  }

  private static Field[] getInjectableFields(final Field[] fields, final int index, final int depth) {
    if (index == fields.length)
      return depth == 0 ? EMPTY_FIELDS : new Field[depth];

    final Field field = fields[index];
    boolean hasContext = false;
    for (int i = 0, i$ = injectableAnnotationTypes.length; i < i$; ++i) // [A]
      if (hasContext |= field.isAnnotationPresent(injectableAnnotationTypes[i]))
        break;

    hasContext |= field.isAnnotationPresent(Context.class);

    final Field[] result = getInjectableFields(fields, index + 1, hasContext ? depth + 1 : depth);
    if (hasContext)
      result[depth] = field;

    return result;
  }

  private final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;
  private final ServerRuntimeContext runtimeContext;

  private HttpServletRequest httpServletRequest;
  private HttpServletResponse httpServletResponse;
  private ContainerResponseContextImpl containerResponseContext;

  private ArrayList<ResourceInfoImpl> resourceInfos;

  private ResourceMatches resourceMatches;
  private ResourceMatch resourceMatch;
  private UriInfoImpl uriInfo;

  private HttpHeadersImpl headers;

  ContainerRequestContextImpl(final PropertiesAdapter<HttpServletRequest> propertiesAdapter, final ServerRuntimeContext runtimeContext, final Request request) {
    super(propertiesAdapter, runtimeContext, request);
    this.readerInterceptorProviderFactories = getReaderInterceptorFactoryList();
    this.runtimeContext = runtimeContext;
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
    if (i$ == 0)
      return;

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
      return resourceMatch == null ? null : (T)resourceMatch.getResourceInfo();

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

  @Override
  @SuppressWarnings("unchecked")
  <T>T findInjectableValue(final AnnotatedElement element, final Annotation[] annotations, final Class<T> clazz, final Type type) throws IOException {
    T injectableObject = super.findInjectableValue(element, annotations, clazz, type);
    if (injectableObject != null)
      return injectableObject;

    final Annotation annotation = findInjectableAnnotation(annotations, true);
    if (annotation != null) {
      final Object argument = getParamObject(element, annotation, annotations, clazz, type);
      if (argument instanceof Exception)
        throw new BadRequestException((Exception)argument);

      return (T)argument;
    }

    // The rest of this concerns the message body, and thus only applies to method parameters
    if (!(element instanceof Parameter))
      return null;

    final Providers providers = getProviders();
    final MediaType contentType = getMediaType();
    final MessageBodyReader<?> messageBodyReader = providers.getMessageBodyReader(clazz, type, annotations, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_TYPE); // [JAX-RS 4.2.1]
    if (messageBodyReader == null)
      throw new WebApplicationException("Could not find MessageBodyReader for type: " + clazz.getName());

    // FIXME: Why is there a return type for ReaderInterceptorContext#proceed()? And it's of type Object. What type is ReaderInterceptorContext supposed to return? It should be InputStream, but then it makes it redundant.
    setType(clazz);
    setGenericType(clazz.getGenericSuperclass());
    setAnnotations(annotations);
    return (T)readBody(messageBodyReader);
  }

  @SuppressWarnings("unchecked")
  private Object getParamObject(final AnnotatedElement element, final Annotation annotation, final Annotation[] annotations, final Class<?> clazz, final Type type) {
    final UriInfo uriInfo = getUriInfo();
    if (annotation.annotationType() == QueryParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      return ParameterUtil.convertParameter(clazz, type, annotations, uriInfo.getQueryParameters(decode).get(((QueryParam)annotation).value()), runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == FormParam.class)
      return httpServletRequest.getParameter(((FormParam)annotation).value());

    if (annotation.annotationType() == PathParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final String pathParamNameToMatch = ((PathParam)annotation).value();

      final MultivaluedMap<String,String> pathParameters = uriInfo.getPathParameters(decode);
      final List<String> values = pathParameters.get(pathParamNameToMatch);
      // FIXME: Another useful warning would be: notify if more than 1 @PathParam annotations specify the same name
      if (values == null)
        if (logger.isWarnEnabled())
          logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") not found in URI template of @Path on: " + element);

      if (clazz == PathSegment.class) {
        final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);
        if (!(pathSegments instanceof RandomAccess))
          throw new IllegalStateException();

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
              if (rangeOverlaps(segStart, segEnd, regionStart, regionEnd))
                return pathSegment;

              segStart = segEnd + 1; // add '/' char
            }
          }
        }

        if (logger.isWarnEnabled())
          logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") PathSegment not found in URI template of @Path on: " + element);

        return null;
      }

      if ((Set.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz) || SortedSet.class.isAssignableFrom(clazz)) && (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0] == PathSegment.class || clazz.isArray() && clazz.getComponentType() == PathSegment.class) {
        final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);
        if (!(pathSegments instanceof RandomAccess))
          throw new IllegalStateException();

        int segStart = 0, segEnd;
        final String[] pathParamNames = resourceMatch.getPathParamNames();
        final long[] regionStartEnds = resourceMatch.getRegionStartEnds();
        try {
          final Collection<PathSegment> matchedSegments = clazz.isArray() ? new ArrayList<>() : (Collection<PathSegment>)ParameterUtil.newCollection(clazz);
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
                  if (rangeOverlaps(segStart, segEnd, regionStart, regionEnd)) {
                    matchedSegments.add(pathSegment);
                  }
                  else {
                    break;
                  }
                }
                else if (inRegion = rangeOverlaps(segStart, segEnd, regionStart, regionEnd)) {
                  matchedSegments.add(pathSegment);
                }

                if (++j == pathSegments.size())
                  break OUT;

                segStart = segEnd + 1; // add '/' char
              }
            }
          }

          return clazz.isArray() ? matchedSegments.toArray((Object[])Array.newInstance(clazz.getComponentType(), matchedSegments.size())) : matchedSegments;
        }
        catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
          // FIXME: This error is kind of hidden in the logs, but it should somehow be highlighted to be fixed?!
          if (logger.isErrorEnabled())
            logger.error(e.getMessage(), e);

          return e instanceof InvocationTargetException ? e.getCause() : e;
        }
      }

      return ParameterUtil.convertParameter(clazz, type, annotations, values, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == MatrixParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);
      if (!(pathSegments instanceof RandomAccess))
        throw new IllegalStateException();

      final String matrixParamName = ((MatrixParam)annotation).value();
      final ArrayList<String> matrixParameters = new ArrayList<>();
      for (int i = 0, i$ = pathSegments.size(); i < i$; ++i) { // [RA]
        final PathSegment pathSegment = pathSegments.get(i);
        for (final Map.Entry<String,List<String>> entry : pathSegment.getMatrixParameters().entrySet()) // [S]
          if (matrixParamName.equals(entry.getKey()))
            matrixParameters.addAll(entry.getValue());
      }

      return ParameterUtil.convertParameter(clazz, type, annotations, matrixParameters, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == CookieParam.class) {
      final Map<String,Cookie> cookies = getCookies();
      if (cookies == null)
        return null;

      final String cookieParam = ((CookieParam)annotation).value();
      return cookies.get(cookieParam);
    }

    if (annotation.annotationType() == HeaderParam.class) {
      final String headerParam = ((HeaderParam)annotation).value();
      final List<String> headerStringValue = getHttpHeaders().get(headerParam);
      if (headerStringValue == null)
        return null;

      if (clazz == String.class)
        return getHeaderString(headerParam);

      if (Set.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz) || SortedSet.class.isAssignableFrom(clazz)) {
        try {
          final Class<?> componentType = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];

          final List<?> headerValues = componentType == String.class ? headerStringValue : getHttpHeaders().getMirrorMap().get(headerParam); // FIXME: Should this be unmodifiable?
          // FIXME: Note this does not consider the generic type of the list -- should it try to do a conversion if the classes don't match?!
          if (clazz.isAssignableFrom(MirrorQualityList.class))
            return headerValues;

          @SuppressWarnings("rawtypes")
          final Collection list = ParameterUtil.newCollection(clazz);
          list.addAll(headerValues);
          return list;
        }
        catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
          // FIXME: This error is kind of hidden in the logs, but it should somehow be highlighted to be fixed?!
          if (logger.isErrorEnabled())
            logger.error(e.getMessage(), e);

          return e instanceof InvocationTargetException ? e.getCause() : e;
        }
      }

      if (clazz.isArray()) {
        // FIXME: Note this does not consider the generic type of the list -- should it try to do a conversion if the classes don't match?!
        final Class<?> componentType = clazz.getComponentType();
        final List<?> headerValues = componentType == String.class ? headerStringValue : getHttpHeaders().getMirrorMap().get(headerParam);
        return headerValues.toArray((Object[])Array.newInstance(componentType, headerValues.size()));
      }

      final Object headerValue = getHttpHeaders().getMirrorMap().getFirst(headerParam);
      if (headerValue == null)
        throw new BadRequestException("Invalid header value: " + headerParam + ": " + headerStringValue.get(0));

      if (clazz.isAssignableFrom(headerValue.getClass()))
        return headerValue;

      return ParameterUtil.convertParameter(clazz, type, annotations, headerStringValue, runtimeContext.getParamConverterProviderFactories(), this);
    }

    throw new UnsupportedOperationException("Unsupported param annotation type: " + annotation.annotationType());
  }

  private static boolean rangeOverlaps(final int startA, final int endA, final int startB, final int endB) {
    return startA <= endB && startB <= endA;
  }

  private static boolean matches(final String pathParamNameToMatch, final String pathParamName) {
    return pathParamName.startsWith(pathParamNameToMatch + UriTemplate.DEL) && pathParamName.lastIndexOf(UriTemplate.DEL, pathParamName.length() - 1) == pathParamNameToMatch.length();
  }

  boolean filterAndMatch() {
    final ResourceMatches resourceMatches = filterAndMatch(httpServletRequest.getMethod(), false);
    if (resourceMatches == null)
      return false;

    if (resourceMatches.size() > 1 && resourceMatches.get(0).compareTo(resourceMatches.get(1)) == 0 && logger.isWarnEnabled()) {
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
    return true;
  }

  private static int[] normalizeUri(final StringBuilder requestUriBuilder, final String path, final boolean inMatrix, final int start, final int depth) {
    for (int i = start, i$ = path.length(); i < i$; ++i) { // [N]
      final char ch = path.charAt(i);
      if (inMatrix) {
        if (ch != '/')
          continue;

        requestUriBuilder.append(ch);
        final int[] ret = normalizeUri(requestUriBuilder, path, false, i + 1, depth + 1);
//        ret[depth] = i;
        return ret;
      }
      else if (ch == ';') {
        final int[] ret = normalizeUri(requestUriBuilder, path, true, i + 1, depth + 1);
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

    normalizeUri(requestUriBuilder, uriInfo.getPath(), false, 0, 0);

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

      if (resourceInfo.getHttpMethod() == null)
        throw new UnsupportedOperationException("JAX-RS 2.1 3.4.1");

      final String resourceMethod = resourceInfo.getHttpMethod().value();
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
      final CompatibleMediaType[] producedMediaTypes = resourceInfo.getCompatibleAccept(getAcceptableMediaTypes(), getHeaders().get(ACCEPT_CHARSET));
      if (producedMediaTypes == null)
        continue;

      if (resourceMatches == null)
        resourceMatches = new ResourceMatches();

      final String[] pathParamNames = uriTemplate.getPathParamNames();
      final MultivaluedMap<String,String> pathParameters = new MultivaluedHashMap<>(pathParamNames.length);
      final long[] regionStartEnds = new long[pathParamNames.length];

      for (int j = 0, j$ = pathParamNames.length; j < j$; ++j) { // [A]
        final String pathParamName = pathParamNames[j];
        final String pathParamValue = matcher.group(pathParamName);
        pathParameters.add(pathParamName.substring(0, pathParamName.lastIndexOf(UriTemplate.DEL, pathParamName.length() - 1)), pathParamValue);

        final int start = matcher.start(pathParamName) - baseUriLen;
        final int end = matcher.end(pathParamName) - baseUriLen;
        regionStartEnds[j] = Numbers.Composite.encode(start, end);
      }

      resourceMatches.add(new ResourceMatch(resourceInfo, matcher.group(), producedMediaTypes, pathParamNames, regionStartEnds, pathParameters)); // We only care about the highest quality match of the Accept header
    }

    if (resourceMatches != null) {
      resourceMatches.sort(null);
      return resourceMatches;
    }

    if (maybeNotAllowed == null || isOverride)
      return null;

    if (HttpMethod.OPTIONS.equals(requestMethod)) {
      final Response.ResponseBuilder response = Response.ok();

      for (final String header : maybeNotAllowed) // [S]
        response.header(ACCESS_CONTROL_ALLOW_METHODS, header).header(ALLOW, header);

      final List<String> requestHeaders = getHeaders().get(ACCESS_CONTROL_REQUEST_HEADERS);
      if (requestHeaders != null)
        for (final String requestHeader : requestHeaders) // [L]
          response.header(ACCESS_CONTROL_ALLOW_HEADERS, requestHeader);

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

    if (maybeNotAcceptable)
      throw new NotAcceptableException();

    if (maybeNotSupported)
      throw new NotSupportedException();

    throw new NotAllowedException(Response.status(Response.Status.METHOD_NOT_ALLOWED).allow(maybeNotAllowed).build());
  }

  void service() throws IOException, ServletException {
    final ResourceInfoImpl resourceInfo = resourceMatch.getResourceInfo();
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

  void sendError(final int scInternalServerError) throws IOException {
    httpServletResponse.sendError(scInternalServerError);
  }

  private Response setResponse(final Response response, final Annotation[] annotations) {
    containerResponseContext.setEntityStream(null);

    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    containerResponseHeaders.clear();

    final MultivaluedMap<String,String> responseHeaders = response.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet()) { // [S]
      final List<String> values = entry.getValue();
      if (values instanceof RandomAccess) {
        for (int i = 0, i$ = values.size(); i < i$; ++i) // [RA]
          containerResponseHeaders.add(entry.getKey(), values.get(i));
      }
      else {
        for (final String value : values) // [L]
          containerResponseHeaders.add(entry.getKey(), value);
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

  private void writeHeader() {
    for (final Map.Entry<String,List<String>> entry : containerResponseContext.getStringHeaders().entrySet()) { // [S]
      final List<String> values = entry.getValue();
      final int i$ = values.size();
      if (i$ == 0)
        continue;

      final String name = entry.getKey();
      if (i$ > 1) {
        final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> headerDelegate = HeaderDelegateImpl.lookup(name);
        final char[] delimiters = headerDelegate.getKey().getDelimiters();
        if (delimiters.length > 0) {
          httpServletResponse.setHeader(name, CollectionUtil.toString(values, delimiters[0]));
          continue;
        }
      }

      if (values instanceof RandomAccess) {
        int i = -1;
        if (httpServletResponse.containsHeader(name))
          httpServletResponse.setHeader(name, values.get(++i));

        while (++i < i$)
          httpServletResponse.addHeader(entry.getKey(), values.get(i));
      }
      else {
        final Iterator<String> i = values.iterator();
        if (httpServletResponse.containsHeader(name))
          httpServletResponse.setHeader(name, i.next());

        while (i.hasNext())
          httpServletResponse.addHeader(entry.getKey(), i.next());
      }
    }

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  @SuppressWarnings("rawtypes")
  private ByteArrayOutputStream writeBody() throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return null;

    final Type methodReturnType;
    final Annotation[] methodAnnotations;
    if (resourceMatch != null) {
      final ResourceInfoImpl resourceInfo = resourceMatch.getResourceInfo();
      methodReturnType = resourceInfo.getMethodReturnType();
      methodAnnotations = resourceInfo.getMethodAnnotations();
    }
    else {
      methodReturnType = null;
      methodAnnotations = null;
    }

    final MediaType mediaType = containerResponseContext.getMediaType();
    final MessageBodyWriter messageBodyWriter = getProviders().getMessageBodyWriter(containerResponseContext.getEntityClass(), methodReturnType, methodAnnotations, mediaType != null ? mediaType : MediaType.WILDCARD_TYPE);
    if (messageBodyWriter == null)
      throw new InternalServerErrorException("Could not find MessageBodyWriter for type: " + entity.getClass().getName()); // [JAX-RS 4.2.2 7]

    ByteArrayOutputStream outputStream = null;
    if (containerResponseContext.getOutputStream() == null)
      containerResponseContext.setEntityStream(outputStream = new ByteArrayOutputStream(1024));

    // Start WriterInterceptor process chain
    containerResponseContext.writeBody(messageBodyWriter);

    // [JAX-RS 3.5 and 3.8 9]
    if (containerResponseContext.getMediaType() == null) {
      MediaType contentType;
      if (resourceMatch == null) {
        contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        if (logger.isWarnEnabled())
          logger.warn("Content-Type not specified -- setting to " + MediaType.APPLICATION_OCTET_STREAM);
      }
      else {
        contentType = resourceMatch.getProducedMediaTypes()[0];
        if (contentType.isWildcardSubtype()) {
          if (!contentType.isWildcardType() && !"application".equals(contentType.getType()))
            throw new NotAcceptableException();

          contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
          if (logger.isWarnEnabled())
            logger.warn("Content-Type not specified -- setting to " + MediaType.APPLICATION_OCTET_STREAM);
        }
        else if (contentType.getParameters().size() > 0) {
          contentType = MediaTypes.cloneWithoutParameters(contentType);
        }
      }

      containerResponseContext.setMediaType(contentType);
    }

    return outputStream;
  }

  ByteArrayOutputStream writeResponse() throws IOException {
    ByteArrayOutputStream outputStream = null;
    if (!HttpMethod.HEAD.equals(getMethod()))
      outputStream = writeBody();

    writeHeader(); // Headers have to be written at the end, because WriteInterceptor(s) may modify the response headers.
    return outputStream;
  }

  void commitResponse(final ByteArrayOutputStream outputStream) throws IOException {
    if (httpServletResponse.isCommitted())
      return;

    try {
      if (outputStream != null) {
        final byte[] bytes = outputStream.toByteArray();
        httpServletResponse.addHeader(CONTENT_LENGTH, String.valueOf(bytes.length));
        httpServletResponse.getOutputStream().write(bytes);
      }

      // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
      httpServletResponse.getOutputStream().flush();
    }
    finally {
      // Absolutely positively assert that the streams are closed
      if (hasEntity() && getEntityStream() != null) {
        try {
          getEntityStream().close();
        }
        catch (final Throwable t) {
          if (logger.isErrorEnabled())
            logger.error(t.getMessage(), t);
        }
      }

      if (containerResponseContext.getOutputStream() != null) {
        try {
          containerResponseContext.getOutputStream().close();
        }
        catch (final Throwable t) {
          if (logger.isErrorEnabled())
            logger.error(t.getMessage(), t);
        }
      }
    }
  }

  @Override
  public void setRequestUri(final URI requestUri) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRequestUri(final URI baseUri, final URI requestUri) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getHeaders() {
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
    if (input != null) {
      try {
        if (input instanceof BufferedServletInputStream) {
          hasEntity = ((BufferedServletInputStream)input).size() > 0;
          entityStream = hasEntity ? input : null;
        }
        else {
          if (!input.markSupported())
            input = new BufferedInputStream(input, 1);

          input.mark(1);

          hasEntity = input.read() != -1;
          if (hasEntity) {
            input.reset();
            entityStream = input;
          }
          else {
            input.close();
            entityStream = null;
          }
        }
      }
      catch (final IOException e) {
        throw new InternalServerErrorException(e);
      }
    }
    else {
      hasEntity = false;
      entityStream = null;
    }
  }

  private SecurityContext defaultSecurityContext;
  private SecurityContext securityContext;

  @Override
  public SecurityContext getSecurityContext() {
    return securityContext != null ? securityContext : defaultSecurityContext == null ? defaultSecurityContext = new SecurityContext() {
      private final HttpServletRequest request = getProperties();

      @Override
      public Principal getUserPrincipal() {
        return null;
      }

      @Override
      public boolean isUserInRole(final String role) {
        return false;
      }

      @Override
      public boolean isSecure() {
        return request.isSecure();
      }

      @Override
      public String getAuthenticationScheme() {
        return null;
      }
    } : defaultSecurityContext;
  }

  @Override
  public void setSecurityContext(final SecurityContext context) {
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

    if (interceptorIndex == size && getInputStream() != null)
      lastProceeded = messageBodyReader.readFrom(getType(), getGenericType(), getAnnotations(), getMediaType(), getHeaders(), getInputStream());

    return lastProceeded;
  }

  @Override
  public void close() throws IOException {
    if (entityStream != null)
      entityStream.close();
  }
}