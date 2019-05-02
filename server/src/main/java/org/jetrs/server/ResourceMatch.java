/* Copyright (c) 2018 OpenJAX
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

import javax.ws.rs.core.MediaType;

public class ResourceMatch {
  private final ResourceManifest manifest;
  private final MediaType accept;

  public ResourceMatch(final ResourceManifest manifest, final MediaType accept) {
    this.manifest = manifest;
    if (manifest == null)
      throw new IllegalArgumentException("manifest == null");

    this.accept = accept;
    if (accept == null)
      throw new IllegalArgumentException("accept == null");
  }

  public ResourceManifest getManifest() {
    return this.manifest;
  }

  public MediaType getAccept() {
    return this.accept;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ResourceMatch))
      return false;

    final ResourceMatch that = (ResourceMatch)obj;
    return accept.equals(that.accept) && manifest.equals(that.manifest);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    hashCode *= 31 ^ hashCode + manifest.hashCode();
    hashCode *= 31 ^ hashCode + accept.hashCode();
    return hashCode;
  }
}