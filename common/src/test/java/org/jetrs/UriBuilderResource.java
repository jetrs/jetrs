/* Copyright (c) 2019 JetRS
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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

@Path("/resource")
public class UriBuilderResource {
  @GET
  @Path("method")
  public String get() {
    final UriBuilder builder = UriBuilder.fromPath("/customers/{id}")
      .scheme("http")
      .host("example.com")
      .resolveTemplate("id", "100")
      .queryParam("myParam", "myValue");

    final URI uri = builder.build();
    return uri.toString();
  }

  @GET
  @Path("locator/")
  public String locator() {
    final UriBuilder builder = UriBuilder.fromPath("/customers/{id}")
      .scheme("http")
      .host("{hostname}")
      .queryParam("{queryParam}", "{queryValue}");

    final URI uri = builder.build("example.com", "100", "myParam", "myValue");
    return uri.toString();
  }

  @GET
  @Path("/test3/")
  public String test3UriBuilder() {
    final UriBuilder builder = UriBuilder.fromPath("/customers/{id}")
      .scheme("http")
      .host("{hostname}")
      .queryParam("{queryParam}", "{queryValue}");

    final Map<String,String> map = new HashMap<>();
    map.put("hostname", "example.com");
    map.put("id", "100");
    map.put("queryParam", "myParam");
    map.put("queryValue", "myValue");

    final URI uri = builder.buildFromMap(map);
    return uri.toString();
  }
}