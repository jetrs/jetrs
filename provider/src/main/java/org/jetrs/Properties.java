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

package org.jetrs;

import org.libj.lang.Booleans;
import org.libj.lang.Numbers;

public final class Properties {
  public static int getPropertyValue(final String commonProperty, final String specificProperty, final int defaultValue) {
    String value = System.getProperty(specificProperty);
    if (value != null)
      return Numbers.parseInt(value, defaultValue);

    return getPropertyValue(commonProperty, defaultValue);
  }

  public static int getPropertyValue(final String commonProperty, final int defaultValue) {
    String value = System.getProperty(commonProperty);
    if (value != null)
      return Numbers.parseInt(value, defaultValue);

    return defaultValue;
  }

  public static boolean getPropertyValue(final String commonProperty, final String specificProperty, final boolean defaultValue) {
    String value = System.getProperty(specificProperty);
    if (value != null)
      return Booleans.parseBoolean(value, defaultValue);

    return getPropertyValue(commonProperty, defaultValue);
  }

  public static boolean getPropertyValue(final String commonProperty, final boolean defaultValue) {
    String value = System.getProperty(commonProperty);
    if (value != null)
      return Booleans.parseBoolean(value, defaultValue);

    return defaultValue;
  }

  public static String getPropertyValue(final String commonProperty, final String specificProperty) {
    final String value = System.getProperty(specificProperty);
    return value != null ? value : System.getProperty(commonProperty);
  }

  private Properties() {
  }
}