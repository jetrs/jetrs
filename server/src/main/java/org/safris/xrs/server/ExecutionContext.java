/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.safris.xrs.server.ext.ProvidersImpl;

public class ExecutionContext {
  private final MultivaluedMap<String,ResourceManifest> resources;
  private final ContainerFilters containerFilters;
  private final Providers providers;
  private final List<ParamConverterProvider> paramConverterProviders;

  public ExecutionContext(final MultivaluedMap<String,ResourceManifest> registry, final ContainerFilters containerFilters, final ProvidersImpl providers, final List<ParamConverterProvider> paramConverterProviders) {
    this.resources = registry;
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

  public List<ParamConverterProvider> getParamConverterProviders() {
    return paramConverterProviders;
  }

  public PathPattern findPathPattern(final String path, final String method) {
    for (final ResourceManifest manifest : resources.get(method))
      if (manifest.getPathPattern().matches(path))
        return manifest.getPathPattern();

    return null;
  }

  public ResourceManifest filterAndMatch(final RequestMatchParams matchParams) {
    final List<ResourceManifest> manifests = resources.get(matchParams.getMethod());
    if (manifests == null)
      return null;

    for (final ResourceManifest manifest : manifests)
      if (manifest.matches(matchParams))
        return manifest;

    return null;
  }
}