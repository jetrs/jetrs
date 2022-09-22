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

package org.jetrs.server.app.provider;

import static org.junit.Assert.*;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jetrs.provider.ext.CharacterProvider;

@Provider
@Singleton
public class MyCharacterProvider extends CharacterProvider {
  public static int instanceCount = 0;
  public static int postConstructCalled = 0;

  @Context
  private HttpServletRequest request;

  @PostConstruct
  private void postConstruct() {
    ++postConstructCalled;
    assertNotNull(request);
  }

  public MyCharacterProvider() {
    ++instanceCount;
  }
}