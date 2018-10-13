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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.Test;
import org.openjax.xrs.server.ext.RuntimeDelegateImpl;

public class MetiaTypesTest {
  static {
    System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
  }

  @Test
  public void testParse() {
    assertEquals(new MediaType("application", "json"), MediaType.valueOf("application/json"));
    assertEquals(new MediaType("application", "json", "utf8"), MediaType.valueOf("application/json; charset=utf8"));
    assertEquals(new MediaType("application", "json", Collections.singletonMap("charset", "utf8")), MediaType.valueOf("application/json; charset=utf8"));
  }

  @Test
  public void testMatch() {
    MediaTypes.matches(new MediaType(), new MediaType());
    MediaTypes.matches(new MediaType("application", "json"), new MediaType());
    MediaTypes.matches(new MediaType(), new MediaType("application", "json"));
    MediaTypes.matches(new MediaType("application", "json"), new MediaType("application", "json"));
  }
}