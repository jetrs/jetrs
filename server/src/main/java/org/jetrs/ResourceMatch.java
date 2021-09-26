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

import static org.libj.lang.Assertions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.net.URLs;

class ResourceMatch implements Comparable<ResourceMatch> {
  private static final Comparator<MultivaluedMap<String,String>> PATH_PARAMETERS_COMPARATOR = new Comparator<MultivaluedMap<String,String>>() {
    @Override
    public int compare(final MultivaluedMap<String,String> o1, final MultivaluedMap<String,String> o2) {
      int s1 = o1.size();
      int s2 = o2.size();
      if (s1 > s2)
        return -1;

      if (s1 < s2)
        return 1;

      s1 = o1.values().toString().length();
      s2 = o2.values().toString().length();
      if (s1 > s2)
        return -1;

      if (s1 < s2)
        return 1;

      return 0;
    }
  };

  private final ResourceManifest manifest;
  private final Class<?> resourceClass;
  private Object singleton;

  private final String uriEncoded;
  private String uriDecoded;
  private final CompatibleMediaType accept;
  private final MultivaluedMap<String,String> pathParameters;

  ResourceMatch(final ResourceManifest manifest, final String uriEncoded, final CompatibleMediaType accept, final MultivaluedMap<String,String> pathParameters) {
    this.manifest = assertNotNull(manifest);
    this.resourceClass = manifest.getResourceClass();
    this.singleton = manifest.getSingleton();

    this.uriEncoded = assertNotNull(uriEncoded);
    this.accept = assertNotNull(accept);
    this.pathParameters = assertNotNull(pathParameters);
  }

  ResourceManifest getManifest() {
    return manifest;
  }

  String getURI(final boolean decode) {
    return !decode ? uriEncoded : uriDecoded == null ? uriDecoded = URLs.decodePath(uriEncoded) : uriDecoded;
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
    int c = manifest.compareTo(o.manifest);
    if (c != 0)
      return c;

    c = PATH_PARAMETERS_COMPARATOR.compare(getPathParameters(), o.getPathParameters());
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