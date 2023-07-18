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

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Variant.VariantListBuilder;

import org.junit.Test;

public class VariantListBuilderTest {
  private static void assertVariants(final VariantListBuilder builder, final Variant ... variants) {
    final List<Variant> actual = builder.build();
    assertEquals(variants.length, actual.size());
    for (final Variant variant : variants)
      assertTrue(actual.contains(variant));

    assertEquals(0, builder.build().size());
  }

  @Test
  public void testEmpty() throws Exception {
    assertVariants(new VariantListBuilderImpl().add());

    assertVariants(new VariantListBuilderImpl());
  }

  @Test
  public void testMediaType() throws Exception {
    final Variant v1 = new Variant(MediaType.TEXT_PLAIN_TYPE, (Locale)null, null);
    final Variant v2 = new Variant(MediaType.TEXT_HTML_TYPE, (Locale)null, null);

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE)
      .add(), v1, v2);

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE), v1, v2);
  }

  @Test
  public void testLocale() throws Exception {
    final Variant v1 = new Variant(null, Locale.ENGLISH, null);
    final Variant v2 = new Variant(null, Locale.FRENCH, null);

    assertVariants(new VariantListBuilderImpl()
      .languages(Locale.ENGLISH, Locale.FRENCH)
      .add(), v1, v2);

    assertVariants(new VariantListBuilderImpl()
      .languages(Locale.ENGLISH, Locale.FRENCH), v1, v2);
  }

  @Test
  public void testEncoding() throws Exception {
    final Variant v1 = new Variant(null, (Locale)null, "utf-8");
    final Variant v2 = new Variant(null, (Locale)null, "utf-16");

    assertVariants(new VariantListBuilderImpl()
      .encodings("utf-8", "utf-16")
      .add(), v1, v2);

    assertVariants(new VariantListBuilderImpl()
      .encodings("utf-8", "utf-16"), v1, v2);
  }

  @Test
  public void testMediaTypeLocale() throws Exception {
    final Variant v1 = new Variant(MediaType.TEXT_PLAIN_TYPE, Locale.ENGLISH, null);
    final Variant v2 = new Variant(MediaType.TEXT_PLAIN_TYPE, Locale.FRENCH, null);
    final Variant v3 = new Variant(MediaType.TEXT_HTML_TYPE, Locale.ENGLISH, null);
    final Variant v4 = new Variant(MediaType.TEXT_HTML_TYPE, Locale.FRENCH, null);

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE)
      .languages(Locale.ENGLISH, Locale.FRENCH)
      .add(), v1, v2, v3, v4);

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE)
      .languages(Locale.ENGLISH, Locale.FRENCH), v1, v2, v3, v4);
  }

  @Test
  public void testMediaTypeEncoding() throws Exception {
    final Variant v1 = new Variant(MediaType.TEXT_PLAIN_TYPE, (Locale)null, "utf-8");
    final Variant v2 = new Variant(MediaType.TEXT_PLAIN_TYPE, (Locale)null, "utf-16");
    final Variant v3 = new Variant(MediaType.TEXT_HTML_TYPE, (Locale)null, "utf-8");
    final Variant v4 = new Variant(MediaType.TEXT_HTML_TYPE, (Locale)null, "utf-16");

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE)
      .encodings("utf-8", "utf-16")
      .add(), v1, v2, v3, v4);

    assertVariants(new VariantListBuilderImpl()
      .mediaTypes(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE)
      .encodings("utf-8", "utf-16"), v1, v2, v3, v4);
  }

  @Test
  public void testLocaleEncoding() throws Exception {
    final Variant v1 = new Variant(null, Locale.ENGLISH, "utf-8");
    final Variant v2 = new Variant(null, Locale.FRENCH, "utf-16");
    final Variant v3 = new Variant(null, Locale.ENGLISH, "utf-8");
    final Variant v4 = new Variant(null, Locale.FRENCH, "utf-16");

    assertVariants(new VariantListBuilderImpl()
      .languages(Locale.ENGLISH, Locale.FRENCH)
      .encodings("utf-8", "utf-16")
      .add(), v1, v2, v3, v4);

    assertVariants(new VariantListBuilderImpl()
      .languages(Locale.ENGLISH, Locale.FRENCH)
      .encodings("utf-8", "utf-16"), v1, v2, v3, v4);
  }
}