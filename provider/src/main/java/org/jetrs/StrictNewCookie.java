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
  private final DirectiveList<CookieDirective> order;
  private final int valueWs;
  private final int pathWs;
  private final int domainWs;
  private final int commentWs;

  StrictNewCookie(final DirectiveList<CookieDirective> order, final String name, final String value, final int valueWs, final String path, final int pathWs, final String domain, final int domainWs, final int version, final String comment, final int commentWs, final int maxAge, final Date expiry, final boolean secure, final boolean httpOnly) {
    super(name, value, path, domain, version, comment, maxAge, expiry, secure, httpOnly);
    this.order = order;
    this.valueWs = valueWs;
    this.pathWs = pathWs;
    this.domainWs = domainWs;
    this.commentWs = commentWs;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append(getName()).append('=').append(getValue());
    final DirectiveList<CookieDirective> order = this.order;
    for (int i = 0, i$ = order.size(); i < i$; ++i) // [RA]
      order.get(i).toString(this, valueWs, pathWs, domainWs, commentWs, b);

    return b.toString();
  }
}