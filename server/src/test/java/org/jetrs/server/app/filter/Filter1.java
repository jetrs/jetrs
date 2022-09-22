package org.jetrs.server.app.filter;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.jetrs.server.app.service.RootService2;

@Provider
public class Filter1 implements ContainerRequestFilter {
  public static int instanceCount = 0;
  public static int postConstructCalled = 0;

  @Context
  private UriInfo uriInfo;

  @Context
  private ResourceInfo resourceInfo;

  public Filter1() {
    ++instanceCount;
  }

  @PostConstruct
  private void postConstruct() {
    ++postConstructCalled;
    assertNotNull(uriInfo);
    assertNotNull(resourceInfo);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    final String baseUri = uriInfo.getBaseUri().getRawPath();
    assertTrue(baseUri.startsWith("/") && baseUri.endsWith("/"));

    final String path = uriInfo.getPath();
    assertFalse("Path does not start with '/'", path.startsWith("/"));
    if ("2/filter".equals(path)) {
      try {
        assertEquals(RootService2.class, resourceInfo.getResourceClass());
        assertEquals(RootService2.class.getMethod("get2Filter", ContainerRequestContext.class), resourceInfo.getResourceMethod());
      }
      catch (final NoSuchMethodException | SecurityException e) {
        throw new InternalServerErrorException(e);
      }
    }
  }
}