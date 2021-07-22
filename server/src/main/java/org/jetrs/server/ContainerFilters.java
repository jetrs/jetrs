/* Copyright (c) 2016 JetRS
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;

import org.jetrs.common.ProviderResource;
import org.jetrs.common.core.AnnotationInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ContainerFilters {
  private static final Logger logger = LoggerFactory.getLogger(ContainerFilters.class);
  private static final int defaultPriority = Priorities.USER;

  private static final Comparator<ProviderResource<?>> priorityComparator = Comparator.nullsFirst((o1, o2) -> {
    final Priority p1 = o1.getProviderClass().getAnnotation(Priority.class);
    final Priority p2 = o2.getProviderClass().getAnnotation(Priority.class);
    final int v1 = p1 != null ? p1.value() : defaultPriority;
    final int v2 = p2 != null ? p2.value() : defaultPriority;
    return Integer.compare(v1, v2);
  });

  private final List<ProviderResource<ContainerRequestFilter>> preMatchContainerRequestFilters = new ArrayList<>();
  private final List<ProviderResource<ContainerRequestFilter>> containerRequestFilters = new ArrayList<>();
  private final List<ProviderResource<ContainerResponseFilter>> containerResponseFilters = new ArrayList<>();

  ContainerFilters(final List<? extends ProviderResource<ContainerRequestFilter>> requestFilters, final List<? extends ProviderResource<ContainerResponseFilter>> responseFilters) {
    for (final ProviderResource<ContainerRequestFilter> requestFilter : requestFilters)
      (requestFilter.getProviderClass().isAnnotationPresent(PreMatching.class) ? preMatchContainerRequestFilters : containerRequestFilters).add(requestFilter);

    for (final ProviderResource<ContainerResponseFilter> responseFilter : responseFilters) {
      if (logger.isDebugEnabled() && responseFilter.getProviderClass().isAnnotationPresent(PreMatching.class))
        logger.debug("@PreMatching annotation is not applicable to ContainerResponseFilter");

      containerResponseFilters.add(responseFilter);
    }

    preMatchContainerRequestFilters.sort(priorityComparator);
    containerRequestFilters.sort(priorityComparator);
    containerResponseFilters.sort(priorityComparator);
  }

  void filterPreMatchContainerRequest(final ContainerRequestContext requestContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerRequestFilter> preMatchRequestFilter : preMatchContainerRequestFilters)
      preMatchRequestFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext);
  }

  void filterContainerRequest(final ContainerRequestContext requestContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerRequestFilter> containerRequestFilter : containerRequestFilters)
      containerRequestFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext);
  }

  void filterContainerResponse(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext, final AnnotationInjector annotationInjector) throws IOException {
    for (final ProviderResource<ContainerResponseFilter> containerResponseFilter : containerResponseFilters)
      containerResponseFilter.getSingletonOrNewInstance(annotationInjector).filter(requestContext, responseContext);
  }
}