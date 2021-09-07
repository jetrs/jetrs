/* Copyright (c) 2021 JetRS
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;

import org.libj.util.TransList;

class ResourceMatches extends ArrayList<ResourceMatch> {
  private static final long serialVersionUID = 3784142771798643436L;

  private final List<String> matchedURIs;
  private final List<Object> matchedResources;

  ResourceMatches(final AnnotationInjector annotationInjector) {
    this.matchedResources = new TransList<>(this, (i, s) -> {
      try {
        return s.getResourceInstance(annotationInjector);
      }
      catch (final IllegalAccessException | InstantiationException e) {
        throw new InternalServerErrorException(e);
      }
      catch (final InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException)
          throw (RuntimeException)e.getCause();

        throw new InternalServerErrorException(e.getCause());
      }
    }, null);
    this.matchedURIs = new TransList<>(this, (i, s) -> {
      return s.getURI();
    }, null);
  }

  @Override
  public boolean add(final ResourceMatch e) {
    return !super.contains(e) && super.add(e);
  }

  List<Object> getMatchedResources() {
    return this.matchedResources;
  }

  // FIXME: Implement `decode`
  List<String> getMatchedURIs(final boolean decode) {
    return this.matchedURIs;
  }
}