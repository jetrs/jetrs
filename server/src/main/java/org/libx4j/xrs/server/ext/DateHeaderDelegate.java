/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.ext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.ext.RuntimeDelegate;

import org.safris.commons.util.Formats;

public class DateHeaderDelegate implements RuntimeDelegate.HeaderDelegate<Date> {
  private static final ThreadLocal<SimpleDateFormat> dateFormat = Formats.createSimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

  public static Date parse(final String value) {
    try {
      return dateFormat.get().parse(value);
    }
    catch (final ParseException e) {
      return null;
    }
  }

  public static String format(final Date value) {
    return dateFormat.get().format(value);
  }

  @Override
  public Date fromString(final String value) {
    return parse(value);
  }

  @Override
  public String toString(final Date value) {
    return format(value);
  }
}