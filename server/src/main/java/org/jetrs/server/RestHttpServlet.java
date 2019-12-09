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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.common.EntityReaderProviderResource;
import org.jetrs.common.EntityWriterProviderResource;
import org.jetrs.common.ExceptionMappingProviderResource;
import org.jetrs.common.ProviderResource;
import org.jetrs.common.ReaderInterceptorEntityProviderResource;
import org.jetrs.common.WriterInterceptorEntityProviderResource;
import org.jetrs.common.ext.ProvidersImpl;
import org.jetrs.server.ext.ServerRuntimeDelegate;

abstract class RestHttpServlet extends HttpServlet {
  private static final long serialVersionUID = 6825431027711735886L;

  private ResourceContext resourceContext;

  protected ResourceContext getResourceContext() {
    return resourceContext;
  }

  private final Application application;

  public RestHttpServlet(final Application application) {
    this.application = application;
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    final MultivaluedMap<String,ResourceManifest> resources = new MultivaluedHashMap<>();
    final List<ExceptionMappingProviderResource> exceptionMappers = new ArrayList<>();
    final List<EntityReaderProviderResource> entityReaders = new ArrayList<>();
    final List<EntityWriterProviderResource> entityWriters = new ArrayList<>();
    final List<ProviderResource<ContainerRequestFilter>> requestFilters = new ArrayList<>();
    final List<ProviderResource<ContainerResponseFilter>> responseFilters = new ArrayList<>();
    final List<ReaderInterceptorEntityProviderResource> readerInterceptors = new ArrayList<>();
    final List<WriterInterceptorEntityProviderResource> writerInterceptors = new ArrayList<>();
    final List<ProviderResource<ParamConverterProvider>> paramConverterProviders = new ArrayList<>();

    final ServerBootstrap bootstrap = new ServerBootstrap();
    try {
      final Application application;
      if (this.application != null) {
        application = this.application;
      }
      else {
        final String applicationSpec = getInitParameter("javax.ws.rs.Application");
        application = applicationSpec == null ? null : (Application)Class.forName(applicationSpec).getDeclaredConstructor().newInstance();
      }

      final Set<?> singletons;
      final Set<Class<?>> classes;
      if (application != null) {
        singletons = application.getSingletons();
        classes = application.getClasses();
      }
      else {
        singletons = null;
        classes = null;
      }

      bootstrap.init(singletons, classes, resources, exceptionMappers, entityReaders, entityWriters, requestFilters, responseFilters, readerInterceptors, writerInterceptors, paramConverterProviders);
      this.resourceContext = new ResourceContext(application, resources, new ContainerFilters(requestFilters, responseFilters), new ProvidersImpl(exceptionMappers, entityReaders, entityWriters), readerInterceptors, writerInterceptors, paramConverterProviders);
      RuntimeDelegate.setInstance(new ServerRuntimeDelegate(this.resourceContext));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}