/* Copyright (c) 2019 OpenJAX
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

package org.jetrs.server.ext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import org.libj.util.Temporals;

public class NewCookieHeaderDelegate implements RuntimeDelegate.HeaderDelegate<NewCookie> {
  @Override
  public NewCookie fromString(final String string) {
    final String[] parts = string.split(";");
    final String part0 = parts[0];
    int index = part0.indexOf('=');
    if (index == -1)
      return null;

    final String name = part0.substring(0, index).trim();
    final String value = part0.substring(index + 1).trim();

    String path = null;
    String domain = null;
    int version = -1;
    String comment = null;
    int maxAge = -1;
    Date expires = null;
    boolean secure = false;
    boolean httpOnly = false;
    for (int i = 1; i < parts.length; ++i) {
      final String part = parts[i];
      if ("Path".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          path = part.substring(index + 1).trim();
      }
      else if ("Domain".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          domain = part.substring(index + 1).trim();
      }
      else if ("Version".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          version = Integer.valueOf(part.substring(index + 1).trim());
      }
      else if ("Comment".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          comment = part.substring(index + 1).trim();
      }
      else if ("Max-Age".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          maxAge = Integer.valueOf(part.substring(index + 1).trim());
      }
      else if ("Expires".equalsIgnoreCase(part)) {
        if ((index = part.indexOf('=')) != -1)
          expires = Temporals.toDate(LocalDateTime.parse(part.substring(index + 1).trim(), DateTimeFormatter.RFC_1123_DATE_TIME));
      }
      else if ("Secure".equalsIgnoreCase(part)) {
        secure = true;
      }
      else if ("HttpOnly".equalsIgnoreCase(part)) {
        httpOnly = true;
      }
    }

    return new NewCookie(name, value, path, domain, version, comment, maxAge, expires, secure, httpOnly);
  }

  @Override
  public String toString(final NewCookie value) {
    final StringBuilder builder = new StringBuilder();
    builder.append(value.getName()).append('=').append(value.getValue());
    if (value.getPath() != null)
      builder.append(';').append("Path").append('=').append(value.getPath());

    if (value.getDomain() != null)
      builder.append(';').append("Domain").append('=').append(value.getDomain());

    if (value.getVersion() != -1)
      builder.append(';').append("Version").append('=').append(value.getVersion());

    if (value.getComment() != null)
      builder.append(';').append("Comment").append('=').append(value.getComment());

    if (value.getMaxAge() != -1)
      builder.append(';').append("Max-Age").append('=').append(value.getMaxAge());

    if (value.getExpiry() != null)
      builder.append(';').append("Expiry").append('=').append(value.getExpiry());

    if (value.isSecure())
      builder.append(';').append("Secure");

    if (value.isHttpOnly())
      builder.append(';').append("HttpOnly");

    return builder.toString();
  }
}