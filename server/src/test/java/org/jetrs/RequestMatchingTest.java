/* Copyright (c) 2023 JetRS
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.junit.Test;

public class RequestMatchingTest extends SingleServiceTest {
  public RequestMatchingTest() {
    startServer(AccountService.class);
  }

  public static class AccountService {
    @GET
    @Path("/")
    public Response get() {
      return Response.noContent().build();
    }

    @GET
    @Path("/{email:\\S+@\\S+\\.\\S+}")
    public String get(@PathParam("email") final String email) {
      return email;
    }
  }

  @Test
  public void testPath() {
    final Response response = target("/").get();
    assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());
  }

  @Test
  public void testPathUnencodedFail() {
    final Response response = target("/foo bar@example.com").get();
    assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());
  }

  @Test
  public void testPathEncodedFail() {
    final Response response = target("/foo%20bar@example.com").get();
    assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());
  }

  @Test
  public void testPathUnencodedPass() {
    final Response response = target("/foo+bar@example.com").get();
    assertEquals(Response.Status.OK, response.getStatusInfo());
  }

  @Test
  public void testPathEncodedPass() {
    final Response response = target("/foo%2Bbar%40example.com").get();
    assertEquals(Response.Status.OK, response.getStatusInfo());
  }
}