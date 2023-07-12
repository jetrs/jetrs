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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class MediaTypesTest {
  private static void testParse(final Consumer<MediaType[]> c, final String ... headers) {
    c.accept(MediaTypes.parse(headers));
    c.accept(MediaTypes.parse(headers == null ? null : Arrays.asList(headers)));
    c.accept(MediaTypes.parse(headers == null ? null : Collections.enumeration(Arrays.asList(headers))));
  }

  @Test
  public void testError() {
    testParse(Assert::assertNull, (String[])null);
    testParse(m -> assertArrayEquals(new MediaType[0], m), (String)null);
    testParse(m -> assertArrayEquals(new MediaType[0], m), "");
    testParse(m -> assertEquals(3, m.length), "application/json; q=\"oops\" ; charset=\"utf8\";  ", "application/xml; q= ; charset=\"utf8\";  ", "application/json; q=\"oops\" ; charset=\"utf8\";  ");
    testParse(m -> assertEquals(3, m.length), "application/json; q=\"oops\" ; charset;  ", "application/xml; q= ; charset=\"utf8\";  ", "application/json; q=\"oops\" ; charset=\"utf8\";  ");
    testParse(m -> assertEquals(3, m.length), "application/json; q=\"oops\" ; charset;  ", "application/xml; q= ; charset=\"utf8\";  ", "application/json; ;;;");
    testParse(m -> assertEquals(3, m.length), "application/json; q=\"oops\" ; charset;  , application/xml; q= ; charset=\"utf8\";  ", "application/json; ;;;");
    testParse(m -> assertEquals(3, m.length), "application/json; q=\"oops\" ; charset;  , application/xml; q= ; charset=\"utf8\";  ,application/json; ;;;");
  }

  @Test
  public void testParse() {
    assertEquals(new MediaType("application", "json"), MediaTypes.parse("application/json"));
    assertEquals(new MediaType("application", "json", "utf8"), MediaTypes.parse("application/json; charset=utf8"));
    assertEquals(new MediaType("application", "json", Collections.singletonMap("charset", "utf8")), MediaTypes.parse("application/json;charset=utf8 "));
    final Map<String,String> parameters = new HashMap<>();
    parameters.put("charset", "utf8");
    parameters.put("q", ".5");
    assertEquals(new MediaType("application", "json", parameters), MediaTypes.parse("application/json;charset=utf8; q=.5"));
    assertEquals(new MediaType("application", "json", parameters), MediaTypes.parse("application/json;charset=\"utf8\"; q=.5"));
    parameters.put("q", "oops");
    assertEquals(new MediaType("application", "json", parameters), MediaTypes.parse("application/json; q=\"oops\" ; charset=\"utf8\";  "));
  }

  @Test
  public void testCompatibleCombinations() {
    final ServerMediaType ww1 = ServerMediaType.WILDCARD_TYPE;
    final ServerMediaType ws1 = new ServerMediaType("*", "specific");
    final ServerMediaType sw1 = new ServerMediaType("specific", "*");
    final MediaType ww2 = MediaType.WILDCARD_TYPE;
    final MediaType ws2 = new MediaType("*", "specific");
    final MediaType sw2 = new MediaType("specific", "*");

    assertEquals(new MediaType("*", "*"), MediaTypes.getCompatible(ww1, ww2, null));
    assertEquals(new MediaType("*", "specific"), MediaTypes.getCompatible(ww1, ws2, null));
    assertEquals(new MediaType("specific", "*"), MediaTypes.getCompatible(ww1, sw2, null));
    assertEquals(new MediaType("*", "specific"), MediaTypes.getCompatible(ws1, ww2, null));
    assertEquals(new MediaType("*", "specific"), MediaTypes.getCompatible(ws1, ws2, null));
    assertEquals(new MediaType("specific", "specific"), MediaTypes.getCompatible(ws1, sw2, null));
    assertEquals(new MediaType("specific", "specific"), MediaTypes.getCompatible(sw1, ws2, null));
    assertEquals(new MediaType("specific", "*"), MediaTypes.getCompatible(sw1, sw2, null));
    assertEquals(new MediaType("specific", "*"), MediaTypes.getCompatible(sw1, ww2, null));
  }

  @Test
  public void testCompatible() {
    assertEquals(MediaTypes.parse("application/vnd.example+json"), MediaTypes.getCompatible(ServerMediaType.valueOf("application/vnd.example+json"), MediaTypes.parse("application/*+json"), null));
    assertEquals(MediaTypes.parse("application/vnd.example+json"), MediaTypes.getCompatible(ServerMediaType.valueOf("application/*+json"), MediaTypes.parse("application/vnd.example+json"), null));

    ServerMediaType server = ServerMediaType.valueOf("application/xml");
    MediaType client = MediaTypes.parse("application/*");
    assertEquals(server, MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/xml;charset=utf-8;x=3;q=.8");
    assertEquals(MediaTypes.parse("application/xml;charset=utf-8;q=.8;x=3"), MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/json");
    assertNull(MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/json;charset=utf-8");
    assertEquals(server, MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/json;charset=utf-8;qs=.5");
    assertEquals(MediaTypes.parse("application/json;charset=utf-8;qs=.5"), MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/json;charset=utf-8;q=.9");
    assertEquals(MediaTypes.parse("application/json;charset=utf-8;q=.9;qs=.5"), MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/json;charset=utf-8;qs=.3");
    assertEquals(MediaTypes.parse("application/json;charset=utf-8;q=.9;qs=.3"), MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/json;x=3;q=.8");
    assertEquals(MediaTypes.parse("application/json;charset=utf-8;q=.8;qs=.3;x=3"), MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/*;y=foo");
    assertEquals(MediaTypes.parse("application/json;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, Arrays.asList("us-ascii")));
    assertEquals(MediaTypes.parse("application/json;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/hal+json;charset=utf-8;x=3;q=.8");
    assertEquals(MediaTypes.parse("application/hal+json;charset=utf-8;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/hal+xml;charset=utf-8;y=foo");
    assertNull(MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/*+xml;charset=utf-8;x=3;q=.8");
    assertEquals(MediaTypes.parse("application/hal+xml;charset=utf-8;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, null));

    server = ServerMediaType.valueOf("application/*+xml;charset=utf-8;y=foo");
    assertEquals(MediaTypes.parse("application/*+xml;charset=utf-8;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, null));

    client = MediaTypes.parse("application/hal+xml;charset=utf-8;x=3;q=.8");
    assertEquals(MediaTypes.parse("application/hal+xml;charset=utf-8;q=.8;x=3;y=foo"), MediaTypes.getCompatible(server, client, null));
  }
}