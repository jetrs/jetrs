/* Copyright (c) 2023 JetRS
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

class VariantListBuilderImpl extends Variant.VariantListBuilder {
  private final ArrayList<Variant> variants = new ArrayList<>();
  private final ArrayList<MediaType> mediaTypes = new ArrayList<>();
  private final ArrayList<Locale> languages = new ArrayList<>();
  private final ArrayList<String> encodings = new ArrayList<>();

  @Override
  public List<Variant> build() {
    add();
    final ArrayList<Variant> copy = new ArrayList<>(variants);
    variants.clear();
    return copy;
  }

  @Override
  public Variant.VariantListBuilder add() {
    final int i$ = mediaTypes.size();
    final int j$ = languages.size();
    final int k$ = encodings.size();

    if (j$ == 0 && k$ == 0 && i$ == 0)
      return this;

    int i = 0; do {
      final MediaType mediaType = i < i$ ? mediaTypes.get(i) : null;
      int j = 0; do {
        final Locale language = j < j$ ? languages.get(j) : null;
        int k = 0; do {
          final String encoding = k < k$ ? encodings.get(k) : null;
          variants.add(new Variant(mediaType, language, encoding));
        }
        while (++k < k$);
      }
      while (++j < j$);
    }
    while (++i < i$);

    mediaTypes.clear();
    languages.clear();
    encodings.clear();

    return this;
  }

  @Override
  public Variant.VariantListBuilder mediaTypes(final MediaType ... mediaTypes) {
    for (final MediaType mediaType : mediaTypes) // [A]
      this.mediaTypes.add(mediaType);

    return this;
  }

  @Override
  public Variant.VariantListBuilder languages(final Locale ... languages) {
    for (final Locale language : languages) // [A]
      this.languages.add(language);

    return this;
  }

  @Override
  public Variant.VariantListBuilder encodings(final String ... encodings) {
    for (final String encoding : encodings) // [A]
      this.encodings.add(encoding);

    return this;
  }
}