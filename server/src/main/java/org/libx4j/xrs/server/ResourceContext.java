/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server;

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.libx4j.xrs.server.ext.ProvidersImpl;

public class ResourceContext {
  private final MultivaluedMap<String,ResourceManifest> resources;
  private final ContainerFilters containerFilters;
  private final Providers providers;
  private final List<ProviderResource<ParamConverterProvider>> paramConverterProviders;

  public ResourceContext(final MultivaluedMap<String,ResourceManifest> resources, final ContainerFilters containerFilters, final ProvidersImpl providers, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) {
    this.resources = resources;
    this.containerFilters = containerFilters;
    this.providers = providers;
    this.paramConverterProviders = paramConverterProviders;
  }

  public ContainerFilters getContainerFilters() {
    return containerFilters;
  }

  public Providers getProviders() {
    return providers;
  }

  public List<ProviderResource<ParamConverterProvider>> getParamConverterProviders() {
    return paramConverterProviders;
  }

  public PathPattern findPathPattern(final String path, final String method) {
    for (final ResourceManifest manifest : resources.get(method))
      if (manifest.getPathPattern().matches(path))
        return manifest.getPathPattern();

    return null;
  }

  public ResourceMatch[] filterAndMatch(final ContainerRequestContext containerRequestContext) {
    final List<ResourceManifest> manifests = resources.get(containerRequestContext.getMethod());
    return manifests == null ? null : filterAndMatch(containerRequestContext, manifests.iterator(), 0);
  }

  private ResourceMatch[] filterAndMatch(final ContainerRequestContext containerRequestContext, final Iterator<ResourceManifest> manifests, final int depth) {
    if (!manifests.hasNext())
      return depth == 0 ? null : new ResourceMatch[depth];

    final ResourceManifest manifest = manifests.next();
    final MediaType accept = manifest.matches(containerRequestContext);
    if (accept == null)
      return filterAndMatch(containerRequestContext, manifests, depth);

    final ResourceMatch[] matches = filterAndMatch(containerRequestContext, manifests, depth + 1);
    matches[depth] = new ResourceMatch(manifest, accept);
    return matches;
  }
}