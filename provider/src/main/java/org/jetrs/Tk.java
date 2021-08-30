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

import java.util.Arrays;

public final class Tk implements Comparable<Tk> {
  public static final Tk UNDER_CONSTRUCTION;
  public static final Tk DYNAMIC;
  public static final Tk TRACKING_WITH_CONSENT;
  public static final Tk DISREGARDING_DNT;
  public static final Tk GATEWAY_OR_MULTIPLE_PARTIES;
  public static final Tk NOT_TRACKING;
  public static final Tk POTENTIAL_CONSENT;
  public static final Tk TRACKING;
  public static final Tk UPDATED;

  private static int index = 0;

  private static final Tk[] values = {
    UNDER_CONSTRUCTION = new Tk('!', "under construction"),
    DYNAMIC = new Tk('?', "dynamic"),
    TRACKING_WITH_CONSENT = new Tk('C', "tracking with consent"),
    DISREGARDING_DNT = new Tk('D', "disregarding DNT"),
    GATEWAY_OR_MULTIPLE_PARTIES = new Tk('G', "gateway or multiple parties"),
    NOT_TRACKING = new Tk('N', "not tracking"),
    POTENTIAL_CONSENT = new Tk('P', "potential consent"),
    TRACKING = new Tk('T', "tracking"),
    UPDATED = new Tk('U', "updated")
  };

  private static final char[] keys = new char[values.length];

  static {
    for (int i = 0; i < keys.length; ++i)
      keys[i] = values[i].symbol;
  }

  static Tk valueOf(final String key) {
    if (key == null || key.length() != 1)
      return null;

    final int index = Arrays.binarySearch(keys, key.charAt(0));
    return index < 0 ? null : values[index];
  }

  public static Tk[] values() {
    return values;
  }

  private final int ordinal;
  private final char symbol;
  private final String name;
  private final String description;

  private Tk(final char symbol, final String description) {
    this.ordinal = index++;
    this.symbol = symbol;
    this.name = Character.toString(symbol);
    this.description = description;
  }

  public int ordinal() {
    return ordinal;
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public int compareTo(final Tk o) {
    return Character.compare(symbol, o.symbol);
  }

  @Override
  public String toString() {
    return name;
  }
}