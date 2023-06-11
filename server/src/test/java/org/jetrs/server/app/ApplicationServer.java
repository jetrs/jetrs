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

package org.jetrs.server.app;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.provider.ext.interceptor.GZipCodec;
import org.jetrs.provider.ext.mapper.ThrowableMapper;
import org.jetrs.provider.ext.mapper.WebApplicationExceptionMapper;
import org.jetrs.server.app.filter.Filter1;
import org.jetrs.server.app.provider.MyCharacterProvider;
import org.jetrs.server.app.service.BookService;
import org.jetrs.server.app.service.CoreTypeService;
import org.jetrs.server.app.service.FileUploadService;
import org.jetrs.server.app.service.FlushResponseService;
import org.jetrs.server.app.service.RootService1;
import org.jetrs.server.app.service.RootService2;
import org.openjax.jetty.EmbeddedServletContainer;
import org.openjax.jetty.UncaughtServletExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath(ApplicationServer.applicationPath)
public class ApplicationServer extends Application implements AutoCloseable {
  public static final String applicationPath = "/foo";

  private static final Logger logger = LoggerFactory.getLogger(ApplicationServer.class);

  public static final String mimeType = "application/vnd.jetrs.v1+json";

  public static void main(final String[] args) throws Exception {
    try (final ApplicationServer instance = new ApplicationServer(null, null)) {
      instance.container.join();
    }
  }

  private final EmbeddedServletContainer container;
  private final HashSet<Object> singletons = new HashSet<>();
  private final Set<Class<?>> classes = new HashSet<>();

  public ApplicationServer(final Object[] singletons, final Class<?>[] classes) {
    if (singletons != null) {
      Collections.addAll(this.singletons, singletons);
    }
    else {
      // General
      this.singletons.add(new WebApplicationExceptionMapper(true));
      this.singletons.add(new ThrowableMapper<>(true));

      // Specific
      this.singletons.add(new RootService1());
      this.singletons.add(new RootService2());
      this.singletons.add(new FileUploadService());
      this.singletons.add(new BookService());
      this.singletons.add(new FlushResponseService());
    }

    if (classes != null) {
      Collections.addAll(this.classes, classes);
    }
    else {
      this.classes.add(Filter1.class);
      this.classes.add(GZipCodec.class);
      this.classes.add(CoreTypeService.class);
      this.classes.add(MyCharacterProvider.class);
    }

    try {
      this.container = new EmbeddedServletContainer.Builder()
        .withUncaughtServletExceptionHandler(new UncaughtServletExceptionHandler() {
          @Override
          public void uncaughtServletException(final ServletRequest request, final ServletResponse response, final Exception e) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            if (logger.isErrorEnabled()) logger.error(httpServletRequest.getMethod() + " " + httpServletRequest.getPathInfo(), e);
          }
        })
        .withServletInstances(RuntimeDelegate.getInstance().createEndpoint(this, HttpServlet.class))
        .withFilterClasses()
        .build();

      this.container.start();
      System.err.println("[START] http://localhost:" + getContainerPort() + applicationPath);
    }
    catch (final RuntimeException t) {
      close();
      throw t;
    }
    catch (final Exception e) {
      close();
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<Object> getSingletons() {
    return singletons;
  }

  @Override
  public Set<Class<?>> getClasses() {
    return classes;
  }

  public void start() throws Exception {
    container.start();
  }

  public int getContainerPort() {
    return container.getPort();
  }

  @Override
  public void close() {
    if (container != null) {
      try {
        container.close();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}