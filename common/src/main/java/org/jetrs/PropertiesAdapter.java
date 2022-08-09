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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

abstract class PropertiesAdapter<T> {
  abstract Object getProperty(T properties, String name);
  abstract Enumeration<String> getPropertyNames(T properties);
  abstract void setProperty(T properties, String name, Object value);
  abstract void removeProperty(T properties, String name);
  abstract int size(T properties);

  static final PropertiesAdapter<HashMap<String,Object>> MAP_ADAPTER = new PropertiesAdapter<HashMap<String,Object>>() {
    @Override
    Object getProperty(final HashMap<String,Object> properties, final String name) {
      return properties.get(name);
    }

    @Override
    Enumeration<String> getPropertyNames(final HashMap<String,Object> properties) {
      return Collections.enumeration(properties.keySet());
    }

    @Override
    void setProperty(final HashMap<String,Object> properties, final String name, final Object value) {
      properties.put(name, value);
    }

    @Override
    void removeProperty(final HashMap<String,Object> properties, final String name) {
      properties.remove(name);
    }

    @Override
    int size(final HashMap<String,Object> properties) {
      return properties.size();
    }
  };
}