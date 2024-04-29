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

package org.jetrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Request;

import org.libj.lang.Enumerations;

final class ServerRuntimeContext extends RuntimeContext {
  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final ArrayList<ResourceInfoImpl> resourceInfos;

  ServerRuntimeContext(final ConfigurationImpl configuration, final ServletConfig servletConfig, final ServletContext servletContext, final Application application, final ArrayList<ResourceInfoImpl> resourceInfos) {
    super(configuration);
    this.resourceInfos = resourceInfos;
    this.servletConfig = servletConfig;
    this.servletContext = servletContext;
    this.application = application;
  }

  @Override
  ServerComponents getComponents() {
    return (ServerComponents)super.getComponents();
  }

  ServletConfig getServletConfig() {
    return servletConfig;
  }

  ServletContext getServletContext() {
    return servletContext;
  }

  Application getApplication() {
    return application;
  }

  ArrayList<ResourceInfoImpl> getResourceInfos() {
    return resourceInfos;
  }

  private static final PropertiesAdapter<HttpServletRequest> propertiesAdapter = new PropertiesAdapter<HttpServletRequest>() {
    @Override
    Object getProperty(final HttpServletRequest properties, final String name) {
      return properties.getAttribute(name);
    }

    @Override
    Enumeration<String> getPropertyNames(final HttpServletRequest properties) {
      return properties.getAttributeNames();
    }

    @Override
    void setProperty(final HttpServletRequest properties, final String name, final Object value) {
      properties.setAttribute(name, value);
    }

    @Override
    void removeProperty(final HttpServletRequest properties, final String name) {
      properties.removeAttribute(name);
    }

    @Override
    int size(final HttpServletRequest properties) {
      return Enumerations.getSize(properties.getAttributeNames());
    }
  };

  private final ThreadLocal<ContainerRequestContextImpl> threadLocalRequestContext = new ThreadLocal<>();

  @Override
  ContainerRequestContextImpl localRequestContext() {
    return threadLocalRequestContext.get();
  }

  ContainerRequestContextImpl newRequestContext(final Request request) {
    final ContainerRequestContextImpl requestContext = new ContainerRequestContextImpl(propertiesAdapter, this, request) {
      @Override
      public void close() throws IOException {
        threadLocalRequestContext.remove();
        super.close();
      }
    };

    threadLocalRequestContext.set(requestContext);
    return requestContext;
  }
}