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

package org.jetrs.common.ext.delegate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import org.libj.util.Strings;
import org.libj.util.Temporals;

public class NewCookieHeaderDelegate implements RuntimeDelegate.HeaderDelegate<NewCookie> {
  // FIXME: This should be re-implemented with a char-by-char algorithm
  @Override
  public NewCookie fromString(final String string) {
    final String[] parts = string.split(";");
    final String part0 = parts[0];
    int index = part0.indexOf('=');
    if (index == -1)
      return null;

    final String name = Strings.trim(part0.substring(0, index).trim(), '"');
    final String value = Strings.trim(part0.substring(index + 1).trim(), '"');

    String path = null;
    String domain = null;
    int version = -1;
    String comment = null;
    int maxAge = -1;
    Date expires = null;
    boolean secure = false;
    boolean httpOnly = false;
    for (int i = 1; i < parts.length; ++i) {
      final String part = parts[i].trim();
      if (part.startsWith("Path")) {
        if ((index = part.indexOf('=')) != -1)
          path = Strings.trim(part.substring(index + 1).trim(), '"');
      }
      else if (part.startsWith("Domain")) {
        if ((index = part.indexOf('=')) != -1)
          domain = part.substring(index + 1).trim();
      }
      else if (part.startsWith("Version")) {
        if ((index = part.indexOf('=')) != -1)
          version = Integer.valueOf(Strings.trim(part.substring(index + 1).trim(), '"'));
      }
      else if (part.startsWith("Comment")) {
        if ((index = part.indexOf('=')) != -1)
          comment = part.substring(index + 1).trim();
      }
      else if (part.startsWith("Max-Age")) {
        if ((index = part.indexOf('=')) != -1)
          maxAge = Integer.valueOf(part.substring(index + 1).trim());
      }
      else if (part.startsWith("Expires")) {
        if ((index = part.indexOf('=')) != -1)
          expires = Temporals.toDate(LocalDateTime.parse(part.substring(index + 1).trim(), DateTimeFormatter.RFC_1123_DATE_TIME));
      }
      else if (part.startsWith("Secure")) {
        secure = true;
      }
      else if (part.startsWith("HttpOnly")) {
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