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

import javax.servlet.ServletException;
import javax.ws.rs.core.MultivaluedMap;

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
  private Object instance;

  private final String uriEncoded;
  private String uriDecoded;
  private final CompatibleMediaType accept;
  private final String[] pathSegmentParamNames;
  private final MultivaluedMap<String,String> pathParameters;
  private final long[] regionStartEnds;

  ResourceMatch(final ResourceManifest manifest, final String uriEncoded, final CompatibleMediaType accept, final String[] pathSegmentParamNames, final long[] regionStartEnds, final MultivaluedMap<String,String> pathParameters) {
    this.manifest = assertNotNull(manifest);
    this.resourceClass = manifest.getResourceClass();
    this.instance = manifest.getSingleton();

    this.uriEncoded = assertNotNull(uriEncoded);
    this.accept = assertNotNull(accept);

    this.pathSegmentParamNames = assertNotNull(pathSegmentParamNames);
    this.regionStartEnds = assertNotNull(regionStartEnds);
    this.pathParameters = assertNotNull(pathParameters);
  }

  ResourceManifest getManifest() {
    return manifest;
  }

  String[] getPathParamNames() {
    return pathSegmentParamNames;
  }

  long[] getRegionStartEnds() {
    return regionStartEnds;
  }

  String getUriEncoded() {
    return uriEncoded;
  }

  String getUriDecoded() {
    return uriDecoded == null ? uriDecoded = URLs.decodePath(uriEncoded) : uriDecoded;
  }

  Object getResourceInstance(final ServerRequestContext requestContext) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
    return instance == null ? instance = requestContext.newResourceInstance(resourceClass) : instance;
  }

  CompatibleMediaType getAccept() {
    return accept;
  }

  MultivaluedMap<String,String> getPathParameters() {
    return pathParameters;
  }

  Object service(final ServerRequestContext requestContext) throws IOException, ServletException {
    return manifest.service(this, requestContext);
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