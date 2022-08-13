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

import org.libj.lang.Numbers;

final class HeaderUtil {
  /**
   * Parses the quality attribute from a raw header string (i.e. {@code "fr-CH;q=0.8"}), and returns a
   * {@link org.libj.lang.Numbers.Composite composite} {@code long} containing the {@code float} quality value and two
   * {@code short}s representing the start and end indexes of the attribute in the string.
   *
   * @param str The string to parse.
   * @param i The index from which to start parsing.
   * @return A {@link org.libj.lang.Numbers.Composite composite} {@code long} containing the {@code float} quality value and two
   *         {@code short}s representing the start and end indexes of the attribute in the string.
   */
  static long getQualityFromString(final String str, int i) {
    if (str == null)
      return Numbers.Composite.encode(1f, (short)-1, (short)-1);

    boolean dotSeen = false;
    boolean qFinished = false;
    StringBuilder builder = null;
    final int len = str.length();
    int start = -1;
    for (int stage = 1; i <= len; ++i) { // [N]
      final char ch;
      if (i == len || (ch = str.charAt(i)) == ',' || qFinished && ch == ';')
        break;

      if (ch == ';') {
        start = i;
        stage = 1;
      }
      else if (stage == 1) {
        if (ch == 'q')
          stage = 2;
        else if (ch != ' ')
          stage = 0;
      }
      else if (stage == 2) {
        if (ch == '=')
          stage = 3;
        else if (ch != ' ')
          stage = 0;
      }
      else if (stage == 3) {
        if ('0' <= ch && ch <= '9' || (dotSeen = ch == '.' && !dotSeen)) {
          if (!qFinished) {
            if (builder == null)
              builder = new StringBuilder();

            builder.append(ch);
            continue;
          }
        }
        else if (ch == ' ') {
          qFinished |= builder != null && builder.length() > 0;
          continue;
        }

        stage = 0;
        dotSeen = false;
        qFinished = false;
        if (builder != null)
          builder.setLength(0);
      }
    }

    if (builder == null || builder.length() == 0)
      return Numbers.Composite.encode(1f, (short)-1, (short)-1);

    // FIXME: Can we avoid Float.parseFloat?
    final float quality = Float.parseFloat(builder.toString());
    return Numbers.Composite.encode(quality, (short)start, (short)i);
  }

  private HeaderUtil() {
  }
}