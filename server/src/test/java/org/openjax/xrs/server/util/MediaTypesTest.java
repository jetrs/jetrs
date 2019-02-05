/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.Test;
import org.openjax.xrs.server.ext.RuntimeDelegateImpl;

public class MediaTypesTest {
  static {
    System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
  }

  @Test
  public void testParse() {
    assertEquals(new MediaType("application", "json"), MediaType.valueOf("application/json"));
    assertEquals(new MediaType("application", "json", "utf8"), MediaType.valueOf("application/json; charset=utf8"));
    assertEquals(new MediaType("application", "json", Collections.singletonMap("charset", "utf8")), MediaType.valueOf("application/json;charset=utf8 "));
    final Map<String,String> parameters = new HashMap<>();
    parameters.put("charset", "utf8");
    parameters.put("q", ".5");
    assertEquals(new MediaType("application", "json", parameters), MediaType.valueOf("application/json;charset=utf8; q=.5"));
    assertEquals(new MediaType("application", "json", parameters), MediaType.valueOf("application/json;charset=\"utf8\"; q=.5"));
    parameters.put("q", "oops;hello=.5;yes!");
    assertEquals(new MediaType("application", "json", parameters), MediaType.valueOf("application/json; q=\"oops;hello=.5;yes!\" ; charset=\"utf8\";  "));
  }

  private static void same(final MediaType specific, final MediaType general) {
    assertSame(specific, MediaTypes.getCompatible(specific, general));
    assertSame(specific, MediaTypes.getCompatible(general, specific));
    assertSame(specific, MediaTypes.getCompatible(specific, specific));
  }

  private static void equals(final MediaType specific, final MediaType general) {
    assertEquals(specific, MediaTypes.getCompatible(specific, general));
    assertEquals(specific, MediaTypes.getCompatible(general, specific));
    assertEquals(specific, MediaTypes.getCompatible(specific, specific));
  }

  @Test
  public void testCompatible() {
    same(null, null);

    MediaType mediaType1 = new MediaType();
    same(mediaType1, null);

    mediaType1 = MediaType.valueOf("application/json");
    same(mediaType1, new MediaType());

    mediaType1 = MediaType.valueOf("application/json");
    MediaType mediaType2 = MediaType.valueOf("application/*");
    same(mediaType1, mediaType2);

    mediaType2 = MediaType.valueOf("application/xml");
    assertNull(MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType2 = MediaType.valueOf("application/xml+json");
    same(mediaType1, mediaType2);


    // NOTE: It is not clear whether the first match for a subtype with a suffix should be
    // NOTE: for the prefix+suffix, or the prefix?
//    mediaType2 = MediaType.valueOf("application/json+xml");
//    same(mediaType1, mediaType2);

    mediaType1 = MediaType.valueOf("application/json;charset=utf-8");
    equals(mediaType1, mediaType2);
//
    mediaType1 = MediaType.valueOf("application/json;charset=utf-8;q=.5");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType2 = MediaType.valueOf("application/json;charset=utf-8;q=.8");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType1 = MediaType.valueOf("application/json;charset=utf-8");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType2 = MediaType.valueOf("application/json;charset=utf-8;x=3;q=.8");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8;x=3"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType1 = MediaType.valueOf("application/json;charset=utf-8;y=foo");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8;x=3;y=foo"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType2 = MediaType.valueOf("application/foo+json;charset=utf-8;x=3;q=.8");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8;x=3;y=foo"), MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType1 = MediaType.valueOf("application/bar+json;charset=utf-8;y=foo");
    assertNull(MediaTypes.getCompatible(mediaType1, mediaType2));

    mediaType2 = MediaType.valueOf("application/*+json;charset=utf-8;x=3;q=.8");
    assertEquals(MediaType.valueOf("application/json;charset=utf-8;x=3;y=foo"), MediaTypes.getCompatible(mediaType1, mediaType2));
  }
}