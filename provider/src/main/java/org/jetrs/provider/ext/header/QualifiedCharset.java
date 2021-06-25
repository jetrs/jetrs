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

package org.jetrs.provider.ext.header;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;

import org.jetrs.provider.util.Equatable;
import org.libj.lang.Numbers;

public class QualifiedCharset extends Charset implements Equatable, Qualified {
  // FIXME: This is repeated in ProviderUtil!
  private static final QualifiedCharset defaultCharset = new QualifiedCharset(Charset.forName("UTF-8"));
  private static final QualifiedCharset wildcardCharset = new QualifiedCharset(defaultCharset, "*", null);

  public static QualifiedCharset valueOf(final String value) {
    final int c = value.indexOf(';');
    if (c == -1)
      return new QualifiedCharset(value);

    final long idx = HeaderUtil.getQualityFromString(value, c);
    final float quality = Numbers.Compound.decodeFloat(idx, 0);
    final int start = Numbers.Compound.decodeShort(idx, 2);
    return new QualifiedCharset(value.substring(0, start), quality);
  }

  public static String toString(final Charset charset) {
    if (charset instanceof QualifiedCharset) {
      final QualifiedCharset qualifiedCharset = (QualifiedCharset)charset;
      return qualifiedCharset.getQuality() != null ? qualifiedCharset.charsetName + ";q=" + qualifiedCharset.getQuality() : qualifiedCharset.charsetName;
    }

    return charset.toString();
  }

  private final Charset charset;
  private final String charsetName;
  private final Float quality;

  public QualifiedCharset(final Charset charset, final Float quality) {
    super(charset.displayName(), charset.aliases().toArray(new String[charset.aliases().size()]));
    this.charsetName = charset.displayName();
    this.charset = charset;
    this.quality = quality;
  }

  private QualifiedCharset(final Charset charset, final String charsetName, final Float quality) {
    super(charset.displayName(), charset.aliases().toArray(new String[charset.aliases().size()]));
    this.charsetName = charsetName;
    this.charset = charset;
    this.quality = quality;
  }

  public QualifiedCharset(final String charsetName, final Float quality) {
    this("*".equals(charsetName) ? (quality != null ? new QualifiedCharset(defaultCharset, "*", quality) : wildcardCharset) : Charset.isSupported(charsetName) ? Charset.forName(charsetName) : defaultCharset, charsetName, quality);
  }

  public QualifiedCharset(final Charset charset) {
    this(charset, null);
  }

  public QualifiedCharset(final String charsetName) {
    this(charsetName, null);
  }

  @Override
  public boolean contains(final Charset cs) {
    return charset.contains(cs);
  }

  @Override
  public CharsetDecoder newDecoder() {
    return charset.newDecoder();
  }

  @Override
  public CharsetEncoder newEncoder() {
    return charset.newEncoder();
  }

  @Override
  public Float getQuality() {
    return quality;
  }

  @Override
  public boolean equal(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof QualifiedCharset))
      return false;

    return super.equals(obj) && Objects.equals(quality, ((QualifiedCharset)obj).quality);
  }
}