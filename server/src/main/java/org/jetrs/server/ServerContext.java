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

package org.jetrs.server;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.jetrs.common.ProviderResource;
import org.jetrs.common.ReaderInterceptorEntityProviderResource;
import org.jetrs.common.WriterInterceptorEntityProviderResource;
import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.common.ext.ProvidersImpl;
import org.jetrs.provider.ext.header.CompatibleMediaType;
import org.jetrs.server.core.ServerConfiguration;

public class ServerContext {
  private final Application application;
  private final Configuration configuration;
  private final List<ResourceManifest> resources;
  private final ContainerFilters containerFilters;
  private final ProvidersImpl providers;
  private final ReaderInterceptor[] readerInterceptors;
  private final WriterInterceptor[] writerInterceptors;
  private final List<ProviderResource<ParamConverterProvider>> paramConverterProviders;

  public ServerContext(final Application application, final List<ResourceManifest> resources, final ContainerFilters containerFilters, final ProvidersImpl providers, final List<ReaderInterceptorEntityProviderResource> readerInterceptors, final List<WriterInterceptorEntityProviderResource> writerInterceptors, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) {
    this.application = application;
    this.configuration = new ServerConfiguration(application);
    this.resources = resources;
    this.containerFilters = containerFilters;
    this.providers = providers;
    this.paramConverterProviders = paramConverterProviders;

    if (readerInterceptors.size() > 0) {
      readerInterceptors.sort(ProvidersImpl.providerResourceComparator);
      this.readerInterceptors = new ReaderInterceptor[readerInterceptors.size()];
      for (int i = 0, len = readerInterceptors.size(); i < len; ++i)
        this.readerInterceptors[i] = readerInterceptors.get(i).getMatchInstance();
    }
    else {
      this.readerInterceptors = null;
    }

    if (writerInterceptors.size() > 0) {
      writerInterceptors.sort(ProvidersImpl.providerResourceComparator);
      this.writerInterceptors = new WriterInterceptor[writerInterceptors.size()];
      for (int i = 0, len = readerInterceptors.size(); i < len; ++i)
        this.writerInterceptors[i] = writerInterceptors.get(i).getMatchInstance();
    }
    else {
      this.writerInterceptors = null;
    }
  }

  public Application getApplication() {
    return this.application;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public ContainerFilters getContainerFilters() {
    return containerFilters;
  }

  public Providers getProviders(final AnnotationInjector annotationInjector) {
    return annotationInjector == null ? providers : new ProvidersImpl(providers, annotationInjector);
  }

  public ReaderInterceptor[] getReaderInterceptors() {
    return this.readerInterceptors;
  }

  public WriterInterceptor[] getWriterInterceptors() {
    return this.writerInterceptors;
  }

  public List<ProviderResource<ParamConverterProvider>> getParamConverterProviders() {
    return paramConverterProviders;
  }

  List<ResourceMatch> filterAndMatch(final ContainerRequestContext containerRequestContext) {
    return filterAndMatch(containerRequestContext, containerRequestContext.getMethod(), true);
  }

  private List<ResourceMatch> filterAndMatch(final ContainerRequestContext containerRequestContext, final String methodOverride, final boolean throwException) {
    final UriInfo uri = containerRequestContext.getUriInfo();
    final String path = uri.getPath();
    List<String> maybeNotAllowed = null;
    boolean maybeNotSupported = false;
    boolean maybeNotAcceptable = false;
    List<ResourceMatch> resourceMatches = null;
    for (final ResourceManifest resource : resources) {
      if (!resource.getPathPattern().matches(path))
        continue;

      if (resource.getHttpMethod() == null)
        throw new UnsupportedOperationException("JAX-RS 2.1 3.4.1");

      if (!methodOverride.equals(resource.getHttpMethod().value())) {
        if (throwException) {
          if (maybeNotAllowed == null)
            maybeNotAllowed = new ArrayList<>();

          maybeNotAllowed.add(resource.getHttpMethod().value());
        }

        continue;
      }

      final List<String> acceptCharsets = containerRequestContext.getHeaders().get(HttpHeaders.ACCEPT_CHARSET);
      maybeNotSupported = true;
      if (containerRequestContext.hasEntity() && resource.getCompatibleContentType(containerRequestContext.getMediaType(), acceptCharsets) == null)
        continue;

      maybeNotAcceptable = true;
      final CompatibleMediaType[] accepts = resource.getCompatibleAccept(containerRequestContext.getAcceptableMediaTypes(), acceptCharsets);
      if (accepts == null)
        continue;

      if (resourceMatches == null)
        resourceMatches = new ArrayList<>();

      resourceMatches.add(new ResourceMatch(resource, accepts[0])); // We only care about the highest quality match of the Accept header
    }

    if (resourceMatches != null) {
      resourceMatches.sort(null);
      return resourceMatches;
    }

    if (HttpMethod.OPTIONS.equals(methodOverride)) {
      final StringBuilder allowMethods = new StringBuilder();
      boolean allowContentType = false;
      boolean allowAccept = false;
      for (final ResourceManifest resource : resources) {
        if (!allowContentType) {
          final MediaTypeAnnotationProcessor<Consumes> resourceAnnotationProcessor = resource.getResourceAnnotationProcessor(Consumes.class);
          allowContentType = resourceAnnotationProcessor.getMediaTypes() != null;
        }

        if (!allowAccept) {
          final MediaTypeAnnotationProcessor<Produces> resourceAnnotationProcessor = resource.getResourceAnnotationProcessor(Produces.class);
          allowAccept = resourceAnnotationProcessor.getMediaTypes() != null;
        }

        if (resource.getPathPattern().matches(path))
          allowMethods.append(resource.getHttpMethod().value()).append(',');
      }

      if (allowMethods.length() > 0)
        allowMethods.setLength(allowMethods.length() - 1);

      final String methods = allowMethods.toString();
      final Response.ResponseBuilder response = Response.ok()
        .header(HttpHeaders.ALLOW, methods)
        .header("Access-Control-Allow-Methods", methods);

      final String allow;
      if (allowAccept && allowContentType)
        allow = HttpHeaders.ACCEPT + "," + HttpHeaders.CONTENT_TYPE;
      else if (allowAccept)
        allow = HttpHeaders.ACCEPT;
      else if (allowContentType)
        allow = HttpHeaders.CONTENT_TYPE;
      else
        allow = null;

      if (allow != null)
        response.header("Access-Control-Allow-Headers", allow);

      containerRequestContext.abortWith(response.build());
    }
    else if (HttpMethod.HEAD.equals(methodOverride)) {
      final List<ResourceMatch> resources = filterAndMatch(containerRequestContext, HttpMethod.GET, false);
      if (resources != null)
        return resources;
    }

    if (!throwException)
      return null;

    if (maybeNotAcceptable)
      throw new NotAcceptableException();

    if (maybeNotSupported)
      throw new NotSupportedException();

    if (maybeNotAllowed != null) {
      final String[] allowed = new String[maybeNotAllowed.size() - 1];
      for (int i = 0; i < allowed.length; ++i)
        allowed[i] = maybeNotAllowed.get(i + 1);

      throw new NotAllowedException(maybeNotAllowed.get(0), allowed);
    }

    return null;
  }
}