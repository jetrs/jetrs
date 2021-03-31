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

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.jetrs.common.ProviderResource;
import org.jetrs.common.ReaderInterceptorEntityProviderResource;
import org.jetrs.common.WriterInterceptorEntityProviderResource;
import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.common.ext.ProvidersImpl;
import org.jetrs.server.core.ServerConfiguration;

public class ResourceContext {
  private final Application application;
  private final Configuration configuration;
  private final MultivaluedMap<String,ResourceManifest> resources;
  private final ContainerFilters containerFilters;
  private final ProvidersImpl providers;
  private final ReaderInterceptor[] readerInterceptors;
  private final WriterInterceptor[] writerInterceptors;
  private final List<ProviderResource<ParamConverterProvider>> paramConverterProviders;

  public ResourceContext(final Application application, final MultivaluedMap<String,ResourceManifest> resources, final ContainerFilters containerFilters, final ProvidersImpl providers, final List<ReaderInterceptorEntityProviderResource> readerInterceptors, final List<WriterInterceptorEntityProviderResource> writerInterceptors, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) {
    this.application = application;
    this.configuration = new ServerConfiguration(application);
    this.resources = resources;
    this.containerFilters = containerFilters;
    this.providers = providers;
    this.paramConverterProviders = paramConverterProviders;

    if (readerInterceptors.size() > 0) {
      readerInterceptors.sort(ProvidersImpl.providerResourceComparator);
      this.readerInterceptors = new ReaderInterceptor[readerInterceptors.size()];
      for (int i = 0; i < readerInterceptors.size(); ++i)
        this.readerInterceptors[i] = readerInterceptors.get(i).getMatchInstance();
    }
    else {
      this.readerInterceptors = null;
    }

    if (writerInterceptors.size() > 0) {
      writerInterceptors.sort(ProvidersImpl.providerResourceComparator);
      this.writerInterceptors = new WriterInterceptor[writerInterceptors.size()];
      for (int i = 0; i < readerInterceptors.size(); ++i)
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

  ResourceMatch[] filterAndMatch(final ContainerRequestContext containerRequestContext) {
    List<ResourceManifest> manifests = resources.get(containerRequestContext.getMethod());
    if (manifests == null) {
      if (HttpMethod.HEAD.equals(containerRequestContext.getMethod())) {
        manifests = resources.get(HttpMethod.GET);
      }
      else if (HttpMethod.OPTIONS.equals(containerRequestContext.getMethod())) {
        final String path = containerRequestContext.getUriInfo().getPath();
        final StringBuilder allowMethods = new StringBuilder();
        boolean allowContentType = false;
        boolean allowAccept = false;
        for (final List<ResourceManifest> resources : resources.values()) {
          for (final ResourceManifest resource : resources) {
            if (!allowContentType) {
              final ResourceAnnotationProcessor<Consumes> resourceAnnotationProcessor = resource.getResourceAnnotationProcessor(Consumes.class);
              allowContentType = resourceAnnotationProcessor.getMediaTypes() != null;
            }

            if (!allowAccept) {
              final ResourceAnnotationProcessor<Produces> resourceAnnotationProcessor = resource.getResourceAnnotationProcessor(Produces.class);
              allowAccept = resourceAnnotationProcessor.getMediaTypes() != null;
            }

            if (resource.getPathPattern().matches(path))
              allowMethods.append(resource.getHttpMethod()).append(',');
          }
        }

        if (allowMethods.length() > 0)
          allowMethods.setLength(allowMethods.length() - 1);

        final String methods = allowMethods.toString();
        final Response.ResponseBuilder response = Response.ok();
        response.header(HttpHeaders.ALLOW, methods);
        response.header("Access-Control-Allow-Methods", methods);

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
    }

    return manifests == null ? null : filterAndMatch(containerRequestContext, manifests.iterator(), 0);
  }

  private static ResourceMatch[] filterAndMatch(final ContainerRequestContext containerRequestContext, final Iterator<? extends ResourceManifest> manifests, final int depth) {
    if (!manifests.hasNext())
      return depth == 0 ? null : new ResourceMatch[depth];

    final ResourceManifest manifest = manifests.next();
    final MediaType accept = manifest.getCompatibleAccept(containerRequestContext);
    if (accept == null)
      return filterAndMatch(containerRequestContext, manifests, depth);

    final ResourceMatch[] matches = filterAndMatch(containerRequestContext, manifests, depth + 1);
    matches[depth] = new ResourceMatch(manifest, accept);
    return matches;
  }
}