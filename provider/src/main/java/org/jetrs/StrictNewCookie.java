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

import javax.ws.rs.core.NewCookie;

class StrictNewCookie extends NewCookie {
  enum Directive {
    PATH("Path") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getPath() != null)
          builder.append("; ").append(name).append('=').append(cookie.getPath());
      }
    },
    DOMAIN("Domain") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getDomain() != null)
          builder.append("; ").append(name).append('=').append(cookie.getDomain());
      }
    },
    VERSION("Version") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getVersion() != -1)
          builder.append("; ").append(name).append('=').append(cookie.getVersion());
      }
    },
    COMMENT("Comment") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getComment() != null)
          builder.append("; ").append(name).append('=').append(cookie.getComment());
      }
    },
    MAX_AGE("Max-Age") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getMaxAge() != -1)
          builder.append("; ").append(name).append('=').append(cookie.getMaxAge());
      }
    },
    EXPIRY("Expires") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.getExpiry() != null)
          builder.append("; ").append(name).append('=').append(cookie.getExpiry());
      }
    },
    SECURE("Secure") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.isSecure())
          builder.append("; ").append(name);
      }
    },
    HTTP_ONLY("HttpOnly") {
      @Override
      void toString(final NewCookie cookie, final StringBuilder builder) {
        if (cookie.isHttpOnly())
          builder.append("; ").append(name);
      }
    };

    final String name;

    Directive(final String name) {
      this.name = name;
    }

    abstract void toString(NewCookie cookie, StringBuilder builder);
  }

  final DirectiveList<Directive> order;

  StrictNewCookie(final DirectiveList<Directive> order, final String name, final String cookie, final String path, final String domain, final int version, final String comment, final int maxAge, final Date expiry, final boolean secure, final boolean httpOnly) {
    super(name, cookie, path, domain, version, comment, maxAge, expiry, secure, httpOnly);
    this.order = order;
  }
}