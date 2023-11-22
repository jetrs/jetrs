/* Copyright (c) 2018 JSONx
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

package org.jetrs.provider.ext.mapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.junit.Test;

public class ThrowableMapperTest {
  // FIXME: This test does not validate that the message is formatted correctly. It only asserts there is no alternate exception.
  @Test
  public void test() {
    final StatusType statusType = mock(StatusType.class);
    when(statusType.getFamily()).thenReturn(Status.Family.CLIENT_ERROR);
    final Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(400);
    when(response.getStatusInfo()).thenReturn(statusType);
    final NullPointerException exception = new NullPointerException("foo\"bar\n\t\r\"'");
    try (final Response res = new ThrowableMapper<>().toResponse(exception, response)) {
      fail("Expected RuntimeException with ClassNotFoundException");
    }
    catch (final RuntimeException e) {
      // FIXME: System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, ??.class.getName());
      assertSame(ClassNotFoundException.class, e.getCause().getClass());
    }
  }
}