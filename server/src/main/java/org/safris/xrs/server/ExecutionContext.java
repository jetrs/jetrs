package org.safris.xrs.server;

import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

public class ExecutionContext {
  private final MultivaluedMap<String,ResourceManifest> resources;
  private final ContainerFilters containerFilters;
  private final EntityProviders entityProviders;

  public ExecutionContext(final MultivaluedMap<String,ResourceManifest> registry, final ContainerFilters containerFilters, final EntityProviders entityProviders) {
    this.resources = registry;
    this.containerFilters = containerFilters;
    this.entityProviders = entityProviders;
  }

  public ContainerFilters getContainerFilters() {
    return containerFilters;
  }

  public EntityProviders getEntityProviders() {
    return entityProviders;
  }

  public ResourceManifest filterAndMatch(final ContainerRequestContext containerRequestContext) {
    final List<ResourceManifest> manifests = resources.get(containerRequestContext.getMethod().toUpperCase());
    if (manifests == null)
      return null;

    for (final ResourceManifest manifest : manifests)
      if (manifest.matches(containerRequestContext))
        return manifest;

    return null;
  }
}