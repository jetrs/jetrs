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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Link;

import org.libj.util.CollectionUtil;

final class Links {
  private enum Section {
    SEMICOLON,
    NAME,
    EQ,
    VALUE,
    DONE
  }

  static Link parse(final CharSequence str) {
    final StringBuilder b = new StringBuilder();
    String uri = null;
    final HashMap<String,String> params = new HashMap<>();

    Section section = null;
    String name = null;
    boolean semiSeen = false;
    boolean isQuoted = false;
    for (int i = 0, i$ = str.length(); i < i$; ++i) { // [ST]
      final char ch = str.charAt(i);
      if (section == null) {
        if (Character.isWhitespace(ch))
          continue;

        if (ch != '<')
          throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + str);

        section = Section.SEMICOLON;
        continue;
      }

      if (uri == null) {
        if (ch == '>') {
          uri = b.toString();
          b.setLength(0);
          continue;
        }
      }
      else if (section == Section.SEMICOLON) {
        if (Character.isWhitespace(ch))
          continue;

        if (ch == ';') {
          if (semiSeen)
            throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + str);

          semiSeen = true;
          section = Section.NAME;
          continue;
        }

        throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + str);
      }
      else if (section == Section.NAME) {
        if (ch == '=') {
          name = b.toString();
          b.setLength(0);
          section = Section.VALUE;
          continue;
        }

        if (Character.isWhitespace(ch)) {
          if (b.length() > 0) {
            name = b.toString();
            b.setLength(0);
            section = Section.EQ;
          }

          continue;
        }
      }
      else if (section == Section.EQ) {
        if (Character.isWhitespace(ch))
          continue;

        if (ch == '=') {
          section = Section.VALUE;
          continue;
        }

        throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + str);
      }
      else if (section == Section.VALUE) {
        if (Character.isWhitespace(ch)) {
          if (!isQuoted) {
            if (b.length() > 0) {
              setValue(params, name, b);
              name = null;
              b.setLength(0);
              section = Section.SEMICOLON;
              semiSeen = false;
              continue;
            }
          }
        }
        else if (ch == '"') {
          if (isQuoted) {
            setValue(params, name, b);
            name = null;
            b.setLength(0);
            section = Section.SEMICOLON;
            semiSeen = false;
            isQuoted = false;
            continue;
          }

          if (b.length() == 0) {
            isQuoted = true;
            continue;
          }
        }
        else if (ch == ',') {
          if (!isQuoted) {
            setValue(params, name, b);
            name = null;
            b.setLength(0);
            b.append((char)i);
            section = Section.DONE;
          }
        }
      }
      else if (section == Section.DONE) {
        if (Character.isWhitespace(ch))
          continue;

        if (ch == ',' && isQuoted)
          continue;

        throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + str);
      }

      b.append(ch);
    }

    if (name != null) {
      if (isQuoted)
        b.insert(0, '"');

      setValue(params, name, b);
    }

    return new LinkImpl(URI.create(uri), params);
  }

  private static void setValue(final HashMap<String,String> params, final String name, final StringBuilder b) {
    if (Link.REL.equals(name)) {
      final String value = params.get(name);
      if (value != null)
        b.insert(0, value).insert(value.length(), '\n');
    }

    params.put(name, b.toString());
  }

  static Set<Link> getLinks(final HttpHeadersImpl headers) {
    final MirrorQualityList<Object,String> values = headers.getMirrorMap().get(HttpHeaders.LINK);
    final int size;
    if (values == null || (size = values.size()) == 0)
      return Collections.EMPTY_SET;

    // FIXME: This is creating a new HashSet each time this method is invoked.
    final HashSet<Link> links = new HashSet<>(size);
    for (int i = 0, i$ = values.size(); i < i$; ++i) // [RA]
      links.add((Link)values.get(i));

    return links;
  }

  static boolean hasLink(final HttpHeadersImpl headers, final String relation) {
    return getLink(headers, relation) != null;
  }

  static Link getLink(final HttpHeadersImpl headers, final String relation) {
    final MirrorQualityList<Object,String> values = headers.getMirrorMap().get(HttpHeaders.LINK);
    if (values == null)
      return null;

    if (!CollectionUtil.isRandomAccess(values))
      throw new IllegalStateException(values.getClass().getName());

    for (int i = 0, i$ = values.size(); i < i$; ++i) { // [RA]
      final Link link = (Link)values.get(i);
      if (link.getRels().contains(relation))
        return link;
    }

    return null;
  }

  static Link.Builder getLinkBuilder(final HttpHeadersImpl headers, final String relation) {
    final Link link = getLink(headers, relation);
    return link != null ? Link.fromLink(link) : null;
  }

  private Links() {
  }
}