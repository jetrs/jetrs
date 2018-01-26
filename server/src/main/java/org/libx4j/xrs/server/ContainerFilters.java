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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContainerFilters {
  private static final Logger logger = LoggerFactory.getLogger(ContainerFilters.class);

  private final List<ContainerRequestFilter> preMatchContainerRequestFilters = new ArrayList<ContainerRequestFilter>();
  private final List<ContainerRequestFilter> containerRequestFilters = new ArrayList<ContainerRequestFilter>();
  private final List<ContainerResponseFilter> containerResponseFilters = new ArrayList<ContainerResponseFilter>();

  private static final Comparator<Object> priorityComparator = new Comparator<Object>() {
    @Override
    public int compare(final Object o1, final Object o2) {
      final Priority p1 = o1.getClass().getAnnotation(Priority.class);
      final Priority p2 = o1.getClass().getAnnotation(Priority.class);
      return p1 == null ? p2 == null ? 0 : 1 : p2 == null ? -1 : Integer.compare(p1.value(), p2.value());
    }
  };

  public ContainerFilters(final List<ContainerRequestFilter> requestFilters, final List<ContainerResponseFilter> responseFilters) {
    for (final ContainerRequestFilter requestFilter : requestFilters)
      (requestFilter.getClass().isAnnotationPresent(PreMatching.class) ? preMatchContainerRequestFilters : containerRequestFilters).add(requestFilter);

    for (final ContainerResponseFilter responseFilter : responseFilters) {
      if (responseFilter.getClass().isAnnotationPresent(PreMatching.class))
        logger.warn("@PreMatching annotation is not applicable to ContainerResponseFilter");

      containerResponseFilters.add(responseFilter);
    }

    Collections.sort(preMatchContainerRequestFilters, priorityComparator);
    Collections.sort(containerRequestFilters, priorityComparator);
    Collections.sort(containerResponseFilters, priorityComparator);
  }

  public void filterPreMatchContainerRequest(final ContainerRequestContext requestContext, final ContextInjector injectionContext) throws IOException {
    for (final ContainerRequestFilter preMatchRequestFilter : preMatchContainerRequestFilters)
      injectionContext.inject(preMatchRequestFilter.getClass()).filter(requestContext);
  }

  public void filterContainerRequest(final ContainerRequestContext requestContext, final ContextInjector injectionContext) throws IOException {
    for (final ContainerRequestFilter postMatchRequestFilter : containerRequestFilters)
      injectionContext.inject(postMatchRequestFilter.getClass()).filter(requestContext);
  }

  public void filterContainerResponse(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext, final ContextInjector injectionContext) throws IOException {
    for (final ContainerResponseFilter postMatchResponseFilter : containerResponseFilters)
      injectionContext.inject(postMatchResponseFilter.getClass()).filter(requestContext, responseContext);
  }
}