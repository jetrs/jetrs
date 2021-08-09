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

public enum Tk {
  UNDER_CONSTRUCTION('!', "under construction"),
  DYNAMIC('?', "dynamic"),
  GATEWAY_OR_MULTIPLE_PARTIES('G', "gateway or multiple parties"),
  NOT_TRACKING('N', "not tracking"),
  TRACKING('T', "tracking"),
  TRACKING_WITH_CONSENT('C', "tracking with consent"),
  POTENTIAL_CONSENT('P', "potential consent"),
  DISREGARDING_DNT('D', "disregarding DNT"),
  UPDATED('U', "updated");

  private final char symbol;
  private final String description;

  private Tk(final char symbol, final String description) {
    this.symbol = symbol;
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return Character.toString(symbol);
  }

  public static Tk fromString(final String str) {
    if (str == null || str.length() != 1)
      return null;

    final char ch = str.charAt(0);
    for (final Tk tk : values())
      if (tk.symbol == ch)
        return tk;

    return null;
  }
}