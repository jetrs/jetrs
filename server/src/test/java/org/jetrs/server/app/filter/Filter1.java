package org.jetrs.server.app.filter;

import static org.junit.Assert.*;

import java.io.IOException;

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
  @Context
  private UriInfo uriInfo;

  @Context
  private ResourceInfo resourceInfo;

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    final String baseUri = uriInfo.getBaseUri().getRawPath();
    assertTrue(baseUri.startsWith("/") && baseUri.endsWith("/"));
    assertFalse(uriInfo.getPath().startsWith("/"));
    assertFalse(uriInfo.getPath().endsWith("/"));
    if ("2/filter".equals(uriInfo.getPath())) {
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