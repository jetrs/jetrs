/* Copyright (c) 2018 JetRS
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
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.Assertions;

class ResourceMatch implements Comparable<ResourceMatch> {
  private final ResourceManifest manifest;
  private final Class<?> resourceClass;
  private Object singleton;

  private final String uri;
  private final CompatibleMediaType accept;
  private final MultivaluedMap<String,String> pathParameters;

  ResourceMatch(final ResourceManifest manifest, final String uri, final CompatibleMediaType accept, final MultivaluedMap<String,String> pathParameters) {
    this.manifest = Assertions.assertNotNull(manifest);
    this.resourceClass = manifest.getResourceClass();
    this.singleton = manifest.getSingleton();

    this.uri = Assertions.assertNotNull(uri);
    this.accept = Assertions.assertNotNull(accept);
    this.pathParameters = Assertions.assertNotNull(pathParameters);
  }

  ResourceManifest getManifest() {
    return manifest;
  }

  String getURI() {
    return uri;
  }

  Object getResourceInstance(final AnnotationInjector annotationInjector) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return singleton == null ? singleton = annotationInjector.newResourceInstance(resourceClass) : singleton;
  }

  CompatibleMediaType getAccept() {
    return this.accept;
  }

  MultivaluedMap<String,String> getPathParameters() {
    return pathParameters;
  }

  Object service(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws IOException, ServletException {
    return manifest.service(this, containerRequestContext, annotationInjector, paramConverterProviders);
  }

  @Override
  public int compareTo(final ResourceMatch o) {
    final int c = manifest.compareTo(o.manifest);
    return c != 0 ? c : MediaTypes.QUALITY_COMPARATOR.compare(accept, o.accept);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ResourceMatch))
      return false;

    final ResourceMatch that = (ResourceMatch)obj;
    return manifest.equals(that.manifest);
  }

  @Override
  public int hashCode() {
    return manifest.hashCode();
  }

  @Override
  public String toString() {
    return String.valueOf(manifest);
  }
}