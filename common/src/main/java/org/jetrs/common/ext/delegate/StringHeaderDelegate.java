/* Copyright (c) 2018 JetRS
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

import javax.ws.rs.ext.RuntimeDelegate;

public class StringHeaderDelegate implements RuntimeDelegate.HeaderDelegate<String> {
  public static String parse(final String value) {
    return value;
  }

  public static String format(final String value) {
    return value;
  }

  @Override
  public String fromString(final String value) {
    return value;
  }

  @Override
  public String toString(final String value) {
    return value;
  }
}