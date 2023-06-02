/* Copyright (c) 2022 JetRS
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.jetrs.provider.container.ResponseTimeoutFilter;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.server.app.ApplicationServer;
import org.junit.AfterClass;
import org.junit.Test;

public class ResponseTimeoutFilterTest {
  private static final String success = "Success";
  private static final String failure = "Failure";

  @Singleton
  public static class TestFilter extends ResponseTimeoutFilter {
    public TestFilter(final long timeout) {
      super(timeout);
    }

    @Override
    protected boolean onTimeout(final ContainerRequestContext requestContext, final long elapsed) {
      timedOut.add(requestContext.getUriInfo().getPath());
      return true;
    }
  }

  @Path("delay")
  public static class DelayService {
    @GET
    @Path("{delay:\\d+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getByte(@PathParam("delay") final long delay) throws InterruptedException {
      try {
        Thread.sleep(delay);
        return success;
      }
      catch (final InterruptedException e) {
        throw e;
      }
    }
  }

  private static final long timeout = 200;
  private static final int numTests = 10;
  private static final ApplicationServer server = new ApplicationServer(new Object[] {
    new TestFilter(timeout),
    new DelayService(),
    new StringProvider(),
    new ExceptionMapper<Throwable>() {
      @Override
      public Response toResponse(final Throwable exception) {
        if (exception instanceof ServletException && exception.getCause() instanceof InterruptedException)
          return Response.status(504).entity(failure).build();

        throw new AssertionError();
      }
    }
  }, new Class[0]);
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort() + ApplicationServer.applicationPath;
  private static final Client client;

  private static ArrayList<String> timedOut = new ArrayList<>();

  static {
    client = ClientBuilder.newBuilder().register(new StringProvider()).build();
  }

  private static Future<Response> test(final long delay) {
    return client.target(serviceUrl + "/delay/" + delay).request().async().get();
  }

  @Test
  @SuppressWarnings("unused")
  public void test() throws Exception {
    try {
      new TestFilter(-1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }

    assertEquals(success, test(0).get().readEntity(String.class));
    assertEquals(0, timedOut.size());

    assertEquals(success, test(timeout / 2).get().readEntity(String.class));
    assertEquals(0, timedOut.size());

    for (int i = 0; i < numTests; ++i) { // [N]
      assertEquals(failure, test(timeout + 10).get().readEntity(String.class));
      assertEquals(timedOut.toString(), 1, timedOut.size());
      timedOut.clear();
    }

    final ArrayList<Future<Response>> responses = new ArrayList<>();
    for (int i = 0; i < numTests; ++i) { // [N]
      responses.add(test(timeout / 5));
      responses.add(test(timeout / 3));
      responses.add(test(timeout / 2));
      responses.add(test(timeout / 4));
    }

    for (int i = 0, i$ = responses.size(); i < i$; ++i) // [RA]
      assertEquals(success, responses.get(i).get().readEntity(String.class));

    assertEquals(0, timedOut.size());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}