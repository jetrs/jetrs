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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.InputStreamProvider;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.provider.ext.mapper.WebApplicationExceptionMapper;
import org.jetrs.server.app.service.FileUploadService;
import org.jetrs.server.app.service.RootService1;
import org.libj.util.function.Throwing;
import org.openjax.jetty.EmbeddedServletContainer;
import org.openjax.jetty.UncaughtServletExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("/*")
public class ApplicationServer extends Application implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationServer.class);

  public static final String mimeType = "application/vnd.pano.fire.v1+json";

  public static void main(final String[] args) throws Exception {
    start();
  }

  public static ApplicationServer start() {
    final ApplicationServer instance = new ApplicationServer();
    new Thread() {
      @Override
      public void run() {
        try {
          instance.container.start();
          instance.container.join();
        }
        catch (final Exception e) {
          Throwing.rethrow(e);
        }
      }
    }.start();

    return instance;
  }

  private final EmbeddedServletContainer container;

  public ApplicationServer() {
    try {
      this.container = new EmbeddedServletContainer.Builder()
        .withPort(0)
        .withUncaughtServletExceptionHandler(new UncaughtServletExceptionHandler() {
          @Override
          public void uncaughtServletException(final ServletRequest request, final ServletResponse response, final Exception e) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            logger.error(httpServletRequest.getMethod() + " " + httpServletRequest.getPathInfo(), e);
          }
        })
        .withServletInstances(RuntimeDelegate.getInstance().createEndpoint(this, HttpServlet.class))
        .withFilterClasses()
        .build();

      this.container.start();
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
    final Set<Object> singletons = new HashSet<>();

    // General
    singletons.add(new StringProvider());
    singletons.add(new InputStreamProvider());
    singletons.add(new BytesProvider());
    singletons.add(new WebApplicationExceptionMapper(true));

    // Specific
    singletons.add(new RootService1());
    singletons.add(new FileUploadService());
    return singletons;
  }

  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> classes = new HashSet<>();
    // Must be a class resource, because it has a member @Context reference
    return classes;
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

  public int getContainerPort() {
    return container.getPort();
  }
}