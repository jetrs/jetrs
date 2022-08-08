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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

import org.libj.lang.Classes;
import org.libj.lang.Numbers;
import org.libj.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerRequestContext extends RequestContext {
  private static final Logger logger = LoggerFactory.getLogger(ServerRequestContext.class);

  private static Field[] EMPTY_FIELDS = {};

  final static Field[] getContextFields(final Object instance) {
    return getInjectableFields(Classes.getDeclaredFieldsDeep(instance.getClass()), 0, 0);
  }

  private static Field[] getInjectableFields(final Field[] fields, final int index, final int depth) {
    if (index == fields.length)
      return depth == 0 ? EMPTY_FIELDS : new Field[depth];

    final Field field = fields[index];
    boolean hasContext = false;
    for (int i = 0; i < injectableAnnotationTypes.length; ++i) // [A]
      if (hasContext |= field.isAnnotationPresent(injectableAnnotationTypes[i]))
        break;

    hasContext |= field.isAnnotationPresent(Context.class);

    final Field[] result = getInjectableFields(fields, index + 1, hasContext ? depth + 1 : depth);
    if (hasContext)
      result[depth] = field;

    return result;
  }

  @SuppressWarnings("unchecked")
  private static final Class<Annotation>[] injectableAnnotationTypes = new Class[] {CookieParam.class, HeaderParam.class, MatrixParam.class, PathParam.class, QueryParam.class};
  // FIXME: Support `AsyncResponse` (JAX-RS 2.1 8.2)

  private final ServerRuntimeContext runtimeContext;

  private HttpServletRequest httpServletRequest;
  private HttpServletResponse httpServletResponse;
  private ContainerRequestContextImpl containerRequestContext;
  private ContainerResponseContextImpl containerResponseContext;

  private List<ResourceInfoImpl> resourceInfos;

  private ResourceMatches resourceMatches;
  private ResourceMatch resourceMatch;

  ServerRequestContext(final ServerRuntimeContext runtimeContext, final Request request) {
    super(runtimeContext, request);
    this.runtimeContext = runtimeContext;
  }

  void init(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
    this.resourceInfos = runtimeContext.getResourceInfos();
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = new ContainerResponseContextImpl(httpServletRequest, httpServletResponse, this);
  }

  ResourceMatches getResourceMatches() {
    return resourceMatches;
  }

  ResourceMatch getResourceMatch() {
    return resourceMatch;
  }

  ContainerRequestContextImpl initContainerRequestContext() {
    return containerRequestContext == null ? containerRequestContext = new ContainerRequestContextImpl(httpServletRequest, this) : containerRequestContext;
  }

  ContainerRequestContextImpl getContainerRequestContext() {
    return containerRequestContext;
  }

  ContainerResponseContextImpl getContainerResponseContext() {
    return containerResponseContext;
  }

  private boolean paramConverterProviderCalled = false;

  List<ProviderFactory<ParamConverterProvider>> getParamConverterProviderFactoryList() {
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
    final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories = runtimeContext.getPreMatchContainerRequestFilterProviderFactories();
    final int len = preMatchContainerRequestFilterProviderFactories.size();
    if (len == 0)
      return;

    final ContainerRequestContext containerRequestContext = initContainerRequestContext();
    for (int i = 0; i < len; ++i) // [L]
      preMatchContainerRequestFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(containerRequestContext);
  }

  private boolean requestFilterCalled = false;

  void filterContainerRequest() throws IOException {
    if (requestFilterCalled)
      throw new IllegalStateException();

    requestFilterCalled = true;
    final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories = runtimeContext.getContainerRequestFilterProviderFactories();
    for (int i = 0, len = containerRequestFilterProviderFactories.size(); i < len; ++i) // [L]
      containerRequestFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(containerRequestContext);
  }

  void filterContainerResponse() throws IOException {
    final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories = runtimeContext.getContainerResponseFilterProviderFactories();
    for (int i = 0, len = containerResponseFilterProviderFactories.size(); i < len; ++i) // [L]
      containerResponseFilterProviderFactories.get(i).getSingletonOrFromRequestContext(this).filter(containerRequestContext, containerResponseContext);
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
      return (T)containerRequestContext.getHeaders();

    if (HttpServletRequest.class.isAssignableFrom(clazz))
      return (T)httpServletRequest;

    if (HttpServletResponse.class.isAssignableFrom(clazz))
      return (T)httpServletResponse;

    if (ResourceInfo.class.isAssignableFrom(clazz))
      return resourceMatch == null ? null : (T)resourceMatch.getResourceInfo();

    if (UriInfo.class.isAssignableFrom(clazz))
      return (T)containerRequestContext.getUriInfo();

    if (SecurityContext.class.isAssignableFrom(clazz))
      return (T)containerRequestContext.getSecurityContext();

    if (ContainerRequestContext.class.isAssignableFrom(clazz))
      return (T)containerRequestContext;

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
    final MessageBodyReader<?> messageBodyReader = providers.getMessageBodyReader(clazz, type, annotations, containerRequestContext.getMediaType());
    if (messageBodyReader == null)
      throw new WebApplicationException("Could not find MessageBodyReader for type: " + clazz.getName());

    // FIXME: Why is there a return type for ReaderInterceptorContext#proceed()? And it's of type Object. What type is ReaderInterceptorContext supposed to return? It should be InputStream, but then it makes it redundant.
    containerRequestContext.setType(clazz);
    containerRequestContext.setGenericType(clazz.getGenericSuperclass());
    containerRequestContext.setAnnotations(annotations);
    return (T)containerRequestContext.readBody(messageBodyReader);
  }

  @SuppressWarnings("unchecked")
  private Object getParamObject(final AnnotatedElement element, final Annotation annotation, final Annotation[] annotations, final Class<?> clazz, final Type type) {
    final UriInfo uriInfo = containerRequestContext.getUriInfo();
    if (annotation.annotationType() == QueryParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      return ParameterUtil.convertParameter(clazz, type, annotations, uriInfo.getQueryParameters(decode).get(((QueryParam)annotation).value()), runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == PathParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final String pathParamNameToMatch = ((PathParam)annotation).value();

      final MultivaluedMap<String,String> pathParameters = uriInfo.getPathParameters(decode);
      final List<String> values = pathParameters.get(pathParamNameToMatch);
      // FIXME: Another useful warning would be: notify if more than 1 @PathParam annotations specify the same name
      if (values == null)
        logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") not found in URI template of @Path on: " + element);

      if (clazz == PathSegment.class) {
        final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);
        final String[] pathParamNames = resourceMatch.getPathParamNames();
        final long[] regionStartEnds = resourceMatch.getRegionStartEnds();
        for (int p = 0, segStart = 0, segEnd; p < pathParamNames.length; ++p) { // [A]
          if (matches(pathParamNameToMatch, pathParamNames[p])) {
            final long regionStartEnd = regionStartEnds[p];
            final int regionStart = Numbers.Composite.decodeInt(regionStartEnd, 0);
            final int regionEnd = Numbers.Composite.decodeInt(regionStartEnd, 1);
            for (int i = 0, len = pathSegments.size(); i < len; ++i) { // [L]
              final PathSegment pathSegment = pathSegments.get(i);
              final String path = ((PathSegmentImpl)pathSegment).getPathEncoded();
              segEnd = segStart + path.length();
              if (rangeOverlaps(segStart, segEnd, regionStart, regionEnd))
                return pathSegment;

              segStart = segEnd + 1; // add '/' char
            }
          }
        }

        logger.warn("@PathParam(\"" + pathParamNameToMatch + "\") PathSegment not found in URI template of @Path on: " + element);
        return null;
      }

      if ((Set.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz) || SortedSet.class.isAssignableFrom(clazz)) && (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0] == PathSegment.class || clazz.isArray() && clazz.getComponentType() == PathSegment.class) {
        final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);

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
          logger.error(e.getMessage(), e);
          return e instanceof InvocationTargetException ? e.getCause() : e;
        }
      }

      return ParameterUtil.convertParameter(clazz, type, annotations, values, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == MatrixParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final List<PathSegment> pathSegments = uriInfo.getPathSegments(decode);
      final String matrixParamName = ((MatrixParam)annotation).value();
      final List<String> matrixParameters = new ArrayList<>();
      for (int i = 0, len = pathSegments.size(); i < len; ++i) { // [L]
        final PathSegment pathSegment = pathSegments.get(i);
        for (final Map.Entry<String,List<String>> entry : pathSegment.getMatrixParameters().entrySet()) // [S]
          if (matrixParamName.equals(entry.getKey()))
            matrixParameters.addAll(entry.getValue());
      }

      return ParameterUtil.convertParameter(clazz, type, annotations, matrixParameters, runtimeContext.getParamConverterProviderFactories(), this);
    }

    if (annotation.annotationType() == CookieParam.class) {
      final Map<String,Cookie> cookies = containerRequestContext.getCookies();
      if (cookies == null)
        return null;

      final String cookieParam = ((CookieParam)annotation).value();
      return cookies.get(cookieParam);
    }

    if (annotation.annotationType() == HeaderParam.class) {
      final String headerParam = ((HeaderParam)annotation).value();
      return containerRequestContext.getHeaderString(headerParam);
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
    final ResourceMatches resourceMatches = filterAndMatch(httpServletRequest.getMethod(), true);
    if (resourceMatches == null)
      return false;

    if (resourceMatches.size() > 1 && resourceMatches.get(0).compareTo(resourceMatches.get(1)) == 0) {
      final StringBuilder builder = new StringBuilder("Multiple resources match ambiguously for request to \"" + httpServletRequest.getRequestURI() + "\": {");
      for (int i = 0, len = resourceMatches.size(); i < len; ++i) // [L]
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
    for (int i = start, len = path.length(); i < len; ++i) { // [N]
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

  private ResourceMatches filterAndMatch(final String methodOverride, final boolean throwException) {
    final ContainerRequestContext containerRequestContext = initContainerRequestContext();
    final UriInfo uriInfo = containerRequestContext.getUriInfo();

    // Match request URI with matrix params stripped out
    final StringBuilder requestUriBuilder = new StringBuilder(uriInfo.getBaseUri().getRawPath());
    final int baseUriLen = requestUriBuilder.length();

    normalizeUri(requestUriBuilder, uriInfo.getPath(), false, 0, 0);

    final String requestUriMatched = requestUriBuilder.toString();

    List<String> maybeNotAllowed = null;
    boolean maybeNotSupported = false;
    boolean maybeNotAcceptable = false;
    ResourceMatches resourceMatches = null;
    for (int i = 0, len = resourceInfos.size(); i < len; ++i) { // [L]
      final ResourceInfoImpl resourceInfo = resourceInfos.get(i);
      final UriTemplate uriTemplate = resourceInfo.getUriTemplate();
      final Matcher matcher = uriTemplate.matcher(requestUriMatched);
      if (!matcher.find())
        continue;

      if (resourceInfo.getHttpMethod() == null)
        throw new UnsupportedOperationException("JAX-RS 2.1 3.4.1");

      if (!methodOverride.equals(resourceInfo.getHttpMethod().value())) {
        if (throwException) {
          if (maybeNotAllowed == null)
            maybeNotAllowed = new ArrayList<>();

          maybeNotAllowed.add(resourceInfo.getHttpMethod().value());
        }

        continue;
      }

      final List<String> acceptCharsets = containerRequestContext.getHeaders().get(HttpHeaders.ACCEPT_CHARSET);
      maybeNotSupported = true;
      if (containerRequestContext.hasEntity() && resourceInfo.getCompatibleContentType(containerRequestContext.getMediaType(), acceptCharsets) == null)
        continue;

      maybeNotAcceptable = true;
      final CompatibleMediaType[] accepts = resourceInfo.getCompatibleAccept(containerRequestContext.getAcceptableMediaTypes(), acceptCharsets);
      if (accepts == null)
        continue;

      if (resourceMatches == null)
        resourceMatches = new ResourceMatches();

      final String[] pathParamNames = uriTemplate.getPathParamNames();
      final MultivaluedMap<String,String> pathParameters = new MultivaluedHashMap<>(pathParamNames.length);
      final long[] regionStartEnds = new long[pathParamNames.length];

      for (int j = 0; j < pathParamNames.length; ++j) { // [A]
        final String pathParamName = pathParamNames[j];
        final String pathParamValue = matcher.group(pathParamName);
        pathParameters.add(pathParamName.substring(0, pathParamName.lastIndexOf(UriTemplate.DEL, pathParamName.length() - 1)), pathParamValue);

        final int start = matcher.start(pathParamName) - baseUriLen;
        final int end = matcher.end(pathParamName) - baseUriLen;
        regionStartEnds[j] = Numbers.Composite.encode(start, end);
      }

      resourceMatches.add(new ResourceMatch(resourceInfo, matcher.group(), accepts[0], pathParamNames, regionStartEnds, pathParameters)); // We only care about the highest quality match of the Accept header
    }

    if (resourceMatches != null) {
      resourceMatches.sort(null);
      return resourceMatches;
    }

    if (HttpMethod.OPTIONS.equals(methodOverride)) {
      final StringBuilder allowMethods = new StringBuilder();
      boolean allowContentType = false;
      boolean allowAccept = false;
      for (int i = 0, len = resourceInfos.size(); i < len; ++i) { // [L]
        final ResourceInfoImpl resourceInfo = resourceInfos.get(i);
        if (!allowContentType) {
          final MediaTypeAnnotationProcessor<Consumes> resourceAnnotationProcessor = resourceInfo.getResourceAnnotationProcessor(Consumes.class);
          allowContentType = resourceAnnotationProcessor.getMediaTypes() != null;
        }

        if (!allowAccept) {
          final MediaTypeAnnotationProcessor<Produces> resourceAnnotationProcessor = resourceInfo.getResourceAnnotationProcessor(Produces.class);
          allowAccept = resourceAnnotationProcessor.getMediaTypes() != null;
        }

        if (resourceInfo.getUriTemplate().matcher(requestUriMatched).matches())
          allowMethods.append(resourceInfo.getHttpMethod().value()).append(',');
      }

      if (allowMethods.length() > 0)
        allowMethods.setLength(allowMethods.length() - 1);

      final String methods = allowMethods.toString();
      final Response.ResponseBuilder response = Response.ok()
        .header(HttpHeaders.ALLOW, methods)
        .header("Access-Control-Allow-Methods", methods);

      if (allowAccept && allowContentType)
        response.header("Access-Control-Allow-Headers", HttpHeaders.ACCEPT + "," + HttpHeaders.CONTENT_TYPE);
      else if (allowAccept)
        response.header("Access-Control-Allow-Headers", HttpHeaders.ACCEPT);
      else if (allowContentType)
        response.header("Access-Control-Allow-Headers", HttpHeaders.CONTENT_TYPE);

      throw new AbortFilterChainException(response.build());
    }
    else if (HttpMethod.HEAD.equals(methodOverride)) {
      final ResourceMatches matches = filterAndMatch(HttpMethod.GET, false);
      if (matches != null)
        return matches;
    }

    if (!throwException)
      return null;

    if (maybeNotAcceptable)
      throw new NotAcceptableException();

    if (maybeNotSupported)
      throw new NotSupportedException();

    if (maybeNotAllowed != null) {
      final String[] allowed = new String[maybeNotAllowed.size() - 1];
      for (int i = 0; i < allowed.length; ++i) // [A]
        allowed[i] = maybeNotAllowed.get(i + 1);

      throw new NotAllowedException(maybeNotAllowed.get(0), allowed);
    }

    return null;
  }

  void service() throws IOException, ServletException {
    final ServerMediaType[] mediaTypes = resourceMatch.getResourceInfo().getResourceAnnotationProcessor(Produces.class).getMediaTypes();
    if (mediaTypes != null)
      containerResponseContext.setMediaType(resourceMatch.getAccept());
    else
      containerResponseContext.setStatus(Response.Status.NO_CONTENT.getStatusCode());

    final Object body = resourceMatch.service(this);
    if (body instanceof Response)
      setResponse((Response)body, resourceMatch.getResourceInfo().getMethodAnnotations(), resourceMatch.getAccept());
    else if (body != null)
      containerResponseContext.setEntity(body, resourceMatch.getResourceInfo().getMethodAnnotations(), resourceMatch.getAccept());
  }

  void setAbortResponse(final AbortFilterChainException e) {
    setResponse(e.getResponse(), null, null);
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
          return setResponse(response, null, null);
      }
    }
    while ((cls = cls.getSuperclass()) != null);

    if (t instanceof WebApplicationException)
      return setResponse(((WebApplicationException)t).getResponse(), null, null);

    return null;
  }

  void sendError(final int scInternalServerError) throws IOException {
    httpServletResponse.sendError(scInternalServerError);
  }

  private Response setResponse(final Response response, final Annotation[] annotations, final CompatibleMediaType mediaType) {
    containerResponseContext.setEntityStream(null);

    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    containerResponseHeaders.clear();

    final MultivaluedMap<String,String> responseHeaders = response.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet()) { // [S]
      final List<String> values = entry.getValue();
      for (int i = 0, len = values.size(); i < len; ++i) // [L]
        containerResponseHeaders.add(entry.getKey(), values.get(i));
    }

    // FIXME: Have to hack getting the annotations out of the Response
    final Annotation[] responseAnnotations = ((ResponseImpl)response).annotations;
    final Annotation[] entityAnnotations = annotations == null ? responseAnnotations : responseAnnotations == null ? annotations : ArrayUtil.concat(responseAnnotations, annotations);

    // FIXME: Assuming that if the response has a MediaType that's set, it overrides the method's Produces annotation?!
    containerResponseContext.setEntity(response.hasEntity() ? response.getEntity() : null, entityAnnotations, response.getMediaType() != null ? response.getMediaType() : mediaType);

    if (response.getStatusInfo() != null) {
      containerResponseContext.setStatusInfo(response.getStatusInfo());
    }
    else {
      containerResponseContext.setStatusInfo(null);
      containerResponseContext.setStatus(response.getStatus());
    }

    return response;
  }

  private void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet()) { // [S]
      final List<String> values = entry.getValue();
      final int len = values.size();
      if (len == 0)
        continue;

      int i = -1;
      final String name = entry.getKey();
      if (httpServletResponse.containsHeader(name))
        httpServletResponse.setHeader(name, values.get(++i));

      while (++i < len)
        httpServletResponse.addHeader(entry.getKey(), values.get(i));
    }

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  private ByteArrayOutputStream entityStream;

  @SuppressWarnings("rawtypes")
  private void writeBody() throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return;

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

    final MessageBodyWriter messageBodyWriter = getProviders().getMessageBodyWriter(containerResponseContext.getEntityClass(), methodReturnType, methodAnnotations, containerResponseContext.getMediaType());
    if (messageBodyWriter == null)
      throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());

    if (containerResponseContext.getOutputStream() == null)
      containerResponseContext.setEntityStream(entityStream = new ByteArrayOutputStream(1024));

    // Start WriterInterceptor process chain
    containerResponseContext.writeBody(messageBodyWriter);
  }

  void writeResponse() throws IOException {
    if (!HttpMethod.HEAD.equals(containerRequestContext.getMethod()))
      writeBody();

    writeHeader(); // Headers have to be written at the end, because WriteInterceptor(s) may modify the response headers.
  }

  void commitResponse() throws IOException {
    if (httpServletResponse.isCommitted())
      return;

    try {
      if (entityStream != null) {
        final byte[] bytes = entityStream.toByteArray();
        httpServletResponse.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
        httpServletResponse.getOutputStream().write(bytes);
      }

      // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
      httpServletResponse.getOutputStream().flush();
    }
    finally {
      // Absolutely positively assert that the streams are closed
      if (containerRequestContext.hasEntity() && containerRequestContext.getEntityStream() != null) {
        try {
          containerRequestContext.getEntityStream().close();
        }
        catch (final Throwable t) {
          logger.error(t.getMessage(), t);
        }
      }

      if (containerResponseContext.getOutputStream() != null) {
        try {
          containerResponseContext.getOutputStream().close();
        }
        catch (final Throwable t) {
          logger.error(t.getMessage(), t);
        }
      }
    }
  }
}