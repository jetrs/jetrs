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

import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.container.ResponseTimeoutFilter;
import org.jetrs.server.app.TestAppServer;
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
    protected void onTimeout(final ContainerRequestContext requestContext, final Thread thread, final long elapsed) {
      timedOut.add(requestContext.getUriInfo().getPath());
      thread.interrupt();
    }
  }

  @Path("delay")
  public static class DelayService {
    @GET
    @Path("{delay:\\d+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getByte(@PathParam("delay") final long delay) {
      try {
        Thread.sleep(delay);
        return success;
      }
      catch (final InterruptedException e) {
        throw new AbortFilterChainException(Response.status(504).entity(failure).build());
      }
    }
  }

  private static final long timeout = 200;
  private static final int numTests = 10;
  private static final TestAppServer server = new TestAppServer(new Object[] {
    new TestFilter(timeout),
    new DelayService()
  }, new Class[0]);
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort() + TestAppServer.applicationPath;
  private static final Client client;

  private static final ArrayList<String> timedOut = new ArrayList<>();

  static {
    client = ClientBuilder.newBuilder().build();
  }

  private static Future<Response> test(final long delay) {
    return client.target(serviceUrl + "/delay/" + delay).request().async().get();
  }

  @Test
  @SuppressWarnings("unused")
  public void testLogic() throws Exception {
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

  @Test
  public void testLoad() throws InterruptedException {
    final int noThreads = 5;
    final ExecutorService executor = Executors.newFixedThreadPool(noThreads);
    for (int j = 0; j < noThreads; ++j) { // [N]
      executor.execute(() -> {
        for (int i = 0; i < 20; ++i) { // [N]
          try {
            test(210).get().readEntity(String.class);
          }
          catch (final Exception e) {
            if (e.getCause() instanceof ProcessingException && e.getCause().getCause() instanceof SocketException && "Unexpected end of file from server".equals(e.getCause().getCause().getMessage()))
              return;

            if (e.getCause() instanceof ProcessingException && e.getCause().getCause() instanceof SocketException && "Unexpected end of file from server".equals(e.getCause().getCause().getMessage()))
              return;

            e.printStackTrace();
            System.exit(1);
          }
        }
      });
    }
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}