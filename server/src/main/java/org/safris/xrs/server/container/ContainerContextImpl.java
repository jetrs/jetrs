/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server.container;

import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.safris.xrs.server.ext.DateHeaderDelegate;

abstract class ContainerContextImpl {
  private final Locale locale;

  protected ContainerContextImpl(final Locale locale) {
    this.locale = locale;
  }

  protected abstract MultivaluedMap<String,String> getStringHeaders();

  public final String getHeaderString(final String name) {
    return getStringHeaders().getFirst(name);
  }

  public final Date getDate() {
    final String date = getStringHeaders().getFirst(HttpHeaders.DATE);
    return date == null ? null : DateHeaderDelegate.parse(date);
  }

  public Locale getLanguage() {
    return locale;
  }
}