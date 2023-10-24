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
import javax.ws.rs.core.MediaType;

import org.libj.net.URLs;
import org.libj.util.ArrayUtil;

class ResourceMatch implements Comparable<ResourceMatch> {
  private static final Comparator<MultivaluedArrayMap<String,String>> PATH_PARAMETERS_COMPARATOR = new Comparator<MultivaluedArrayMap<String,String>>() {
    @Override
    public int compare(final MultivaluedArrayMap<String,String> o1, final MultivaluedArrayMap<String,String> o2) {
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

  private final ResourceInfoImpl resourceInfo;
  private final Class<?> resourceClass;
  private Object instance;

  private final String uriEncoded;
  private String uriDecoded;
  private final MediaType[] compatibleMediaTypes;
  private final String[] pathSegmentParamNames;
  private final MultivaluedArrayMap<String,String> pathParameters;
  private final long[] regionStartEnds;

  ResourceMatch(final ResourceInfoImpl resourceInfo, final String uriEncoded, final MediaType[] compatibleMediaTypes, final String[] pathSegmentParamNames, final long[] regionStartEnds, final MultivaluedArrayMap<String,String> pathParameters) {
    this.resourceInfo = resourceInfo;
    this.resourceClass = resourceInfo.getResourceClass();
    this.instance = resourceInfo.getSingleton();

    this.uriEncoded = assertNotNull(uriEncoded);
    this.compatibleMediaTypes = assertNotEmpty(compatibleMediaTypes);

    this.pathSegmentParamNames = assertNotNull(pathSegmentParamNames);
    this.regionStartEnds = assertNotNull(regionStartEnds);
    this.pathParameters = assertNotNull(pathParameters);
  }

  ResourceInfoImpl getResourceInfo() {
    return resourceInfo;
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

  MediaType[] getCompatibleMediaTypes() {
    return compatibleMediaTypes;
  }

  Object getResourceInstance(final ContainerRequestContextImpl requestContext) throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException {
    return instance == null ? instance = requestContext.newResourceInstance(resourceClass) : instance;
  }

  MultivaluedArrayMap<String,String> getPathParameters() {
    return pathParameters;
  }

  Object service(final ContainerRequestContextImpl requestContext) throws IOException, ServletException {
    return resourceInfo.service(this, requestContext);
  }

  @Override
  public int compareTo(final ResourceMatch o) {
    int c = resourceInfo.compareTo(o.resourceInfo);
    if (c != 0)
      return c;

    c = PATH_PARAMETERS_COMPARATOR.compare(getPathParameters(), o.getPathParameters());
    if (c != 0)
      return c;

    c = ArrayUtil.compare(compatibleMediaTypes, o.compatibleMediaTypes, MediaTypes.QUALITY_COMPARATOR);
    if (c != 0)
      return c;

    return ArrayUtil.compare(resourceInfo.getProducesMediaTypes(), o.resourceInfo.getProducesMediaTypes(), MediaTypes.QUALITY_COMPARATOR);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ResourceMatch))
      return false;

    final ResourceMatch that = (ResourceMatch)obj;
    return resourceInfo.equals(that.resourceInfo);
  }

  @Override
  public int hashCode() {
    return resourceInfo.hashCode();
  }

  @Override
  public String toString() {
    return String.valueOf(resourceInfo);
  }
}