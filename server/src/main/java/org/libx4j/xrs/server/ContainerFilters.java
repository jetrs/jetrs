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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

import org.lib4j.util.Collections;
import org.libx4j.xrs.server.core.ContextInjector;

public class ContainerFilters {
  private final List<Class<? extends ContainerResponseFilter>> preMatchResponseFilters = new ArrayList<Class<? extends ContainerResponseFilter>>();
  private final List<Class<? extends ContainerResponseFilter>> postMatchResponseFilters = new ArrayList<Class<? extends ContainerResponseFilter>>();
  private final List<Class<? extends ContainerRequestFilter>> preMatchRequestFilters = new ArrayList<Class<? extends ContainerRequestFilter>>();
  private final List<Class<? extends ContainerRequestFilter>> postMatchRequestFilters = new ArrayList<Class<? extends ContainerRequestFilter>>();

  private static final Comparator<Class<?>> priorityComparator = new Comparator<Class<?>>() {
    @Override
    public int compare(final Class<?> o1, final Class<?> o2) {
      final Priority p1 = o1.getAnnotation(Priority.class);
      final Priority p2 = o1.getAnnotation(Priority.class);
      return p1 == null ? p2 == null ? 0 : 1 : p2 == null ? -1 : Integer.compare(p1.value(), p2.value());
    }
  };

  public ContainerFilters(final List<Class<? extends ContainerRequestFilter>> requestFilters, final List<Class<? extends ContainerResponseFilter>> responseFilters) {
    for (final Class<? extends ContainerRequestFilter> requestFilter : requestFilters)
      (requestFilter.isAnnotationPresent(PreMatching.class) ? preMatchRequestFilters : postMatchRequestFilters).add(requestFilter);

    for (final Class<? extends ContainerResponseFilter> responseFilter : responseFilters)
      (responseFilter.isAnnotationPresent(PreMatching.class) ? preMatchResponseFilters : postMatchResponseFilters).add(responseFilter);

    Collections.sort(preMatchRequestFilters, priorityComparator);
    Collections.sort(postMatchRequestFilters, priorityComparator);
    Collections.sort(preMatchResponseFilters, priorityComparator);
    Collections.sort(postMatchResponseFilters, priorityComparator);
  }

  public void filterPreMatchRequest(final ContainerRequestContext requestContext, final ContextInjector injectionContext) throws IOException {
    for (final Class<? extends ContainerRequestFilter> preMatchRequestFilter : preMatchRequestFilters) {
      final ContainerRequestFilter filter = injectionContext.inject(preMatchRequestFilter);
      filter.filter(requestContext);
    }
  }

  public void filterPostMatchRequest(final ContainerRequestContext requestContext, final ContextInjector injectionContext) throws IOException {
    for (final Class<? extends ContainerRequestFilter> postMatchRequestFilter : postMatchRequestFilters) {
      final ContainerRequestFilter filter = injectionContext.inject(postMatchRequestFilter);
      filter.filter(requestContext);
    }
  }

  public void filterPreMatchResponse(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext, final ContextInjector injectionContext) throws IOException {
    for (final Class<? extends ContainerResponseFilter> preMatchResponseFilter : preMatchResponseFilters) {
      final ContainerResponseFilter filter = injectionContext.inject(preMatchResponseFilter);
      filter.filter(requestContext, responseContext);
    }
  }

  public void filterPostMatchResponse(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext, final ContextInjector injectionContext) throws IOException {
    for (final Class<? extends ContainerResponseFilter> postMatchResponseFilter : postMatchResponseFilters) {
      final ContainerResponseFilter filter = injectionContext.inject(postMatchResponseFilter);
      filter.filter(requestContext, responseContext);
    }
  }
}