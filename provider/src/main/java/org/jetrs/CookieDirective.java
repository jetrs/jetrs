/* Copyright (c) 2021 JetRS
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

import java.util.Date;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.libj.lang.Strings;

enum CookieDirective {
  PATH("Path") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final String path = cookie.getPath();
      if (path != null) {
        builder.append(';').append(name).append('=');
        if (pathWs == 1 || pathWs == -1 && Strings.hasWhitespace(path))
          builder.append('"').append(path).append('"');
        else
          builder.append(path);
      }
    }
  },
  DOMAIN("Domain") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final String domain = cookie.getDomain();
      if (domain != null) {
        builder.append(';').append(name).append('=');
        if (domainWs == 1 || domainWs == -1 && Strings.hasWhitespace(domain))
          builder.append('"').append(domain).append('"');
        else
          builder.append(domain);
      }
    }
  },
  VERSION("Version") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final int version = cookie.getVersion();
      if (version != -1)
        builder.append(';').append(name).append('=').append(version);
    }
  },
  COMMENT("Comment") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final String comment = ((NewCookie)cookie).getComment();
      if (comment != null) {
        builder.append(';').append(name).append('=');
        if (commentWs == 1 || commentWs == -1 && Strings.hasWhitespace(comment))
          builder.append('"').append(comment).append('"');
        else
          builder.append(comment);
      }
    }
  },
  MAX_AGE("Max-Age") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final int maxAge = ((NewCookie)cookie).getMaxAge();
      if (maxAge != -1)
        builder.append(';').append(name).append('=').append(maxAge);
    }
  },
  EXPIRY("Expires") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      final Date expiry = ((NewCookie)cookie).getExpiry();
      if (expiry != null)
        builder.append(';').append(name).append('=').append(expiry);
    }
  },
  SECURE("Secure") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      if (((NewCookie)cookie).isSecure())
        builder.append(';').append(name);
    }
  },
  HTTP_ONLY("HttpOnly") {
    @Override
    void toString(final Cookie cookie, final int valueWs, final int pathWs, final int domainWs, final int commentWs, final StringBuilder builder) {
      if (((NewCookie)cookie).isHttpOnly())
        builder.append(';').append(name);
    }
  };

  static CookieDirective[] cookies = {PATH, DOMAIN, VERSION};

  final String name;

  private CookieDirective(final String name) {
    this.name = name;
  }

  abstract void toString(Cookie cookie, int valueWs, int pathWs, int domainWs, int commentWs, StringBuilder builder);
}