/* Copyright (c) 2018 lib4j
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

import javax.ws.rs.core.MediaType;

public class ResourceMatch {
  private final ResourceManifest manifest;
  private final MediaType accept;

  public ResourceMatch(final ResourceManifest manifest, final MediaType accept) {
    this.manifest = manifest;
    this.accept = accept;
  }

  public ResourceManifest getManifest() {
    return this.manifest;
  }

  public MediaType getAccept() {
    return this.accept;
  }
}