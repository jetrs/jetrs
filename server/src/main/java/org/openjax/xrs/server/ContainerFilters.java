/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server;

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

import org.openjax.xrs.server.core.AnnotationInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContainerFilters {
  private static final Logger logger = LoggerFactory.getLogger(ContainerFilters.class);

  private final List<ProviderResource<ContainerRequestFilter>> preMatchContainerRequestFilters = new ArrayList<>();
  private final List<ProviderResource<ContainerRequestFilter>> containerRequestFilters = new ArrayList<>();
  private final List<ProviderResource<ContainerResponseFilter>> containerResponseFilters = new ArrayList<>();

  private static final Comparator<Object> priorityComparator = Comparator.nullsFirst(new Comparator<Object>() {
    @Override
    public int compare(final Object o1, final Object o2) {
      final Priority p1 = o1.getClass().getAnnotation(Priority.class);
      final Priority p2 = o1.getClass().getAnnotation(Priority.class);
      return p1 == null ? p2 == null ? 0 : 1 : p2 == null ? -1 : Integer.compare(p1.value(), p2.value());
    }
  });

  public ContainerFilters(final List<ProviderResource<ContainerRequestFilter>> requestFilters, final List<ProviderResource<ContainerResponseFilter>> responseFilters) {
    for (final ProviderResource<ContainerRequestFilter> requestFilter : requestFilters)
      (requestFilter.getProviderClass().isAnnotationPresent(PreMatching.class) ? preMatchContainerRequestFilters : containerRequestFilters).add(requestFilter);

    for (final ProviderResource<ContainerResponseFilter> responseFilter : responseFilters) {
      if (responseFilter.getProviderClass().isAnnotationPresent(PreMatching.class))
        logger.warn("@PreMatching annotation is not applicable to ContainerResponseFilter");

      containerResponseFilters.add(responseFilter);
    }

    preMatchContainerRequestFilters.sort(priorityComparator);
    containerRequestFilters.sort(priorityComparator);
    containerResponseFilters.sort(priorityComparator);
  }

  public void filterPreMatchContainerRequest(final ContainerRequestContext requestContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerRequestFilter> preMatchRequestFilter : preMatchContainerRequestFilters)
      preMatchRequestFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext);
  }

  public void filterContainerRequest(final ContainerRequestContext requestContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerRequestFilter> containerRequestFilter : containerRequestFilters)
      containerRequestFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext);
  }

  public void filterContainerResponse(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerResponseFilter> containerResponseFilter : containerResponseFilters)
      containerResponseFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext, responseContext);
  }
}