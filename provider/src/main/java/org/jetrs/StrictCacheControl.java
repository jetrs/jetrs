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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.CacheControl;

import org.libj.lang.Strings;
import org.libj.util.CollectionUtil;

/**
 * An extension of {@link CacheControl} that preserves the order of input-to-output of directives, and vice-versa.
 */
class StrictCacheControl extends CacheControl {
  enum Directive {
    PRIVATE("public", "private") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        if ("public".equals(directive)) {
          cacheControl.setPrivate(false);
          return true;
        }

        final String field = parseFieldValue(directive);
        if (field == null)
          cacheControl.setPrivate(true);
        else
          cacheControl.getPrivateFields().add(field);

        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        final List<String> fields = cacheControl.getPrivateFields();
        final int i$ = fields.size();
        if (i$ > 0) {
          if (CollectionUtil.isRandomAccess(fields)) {
            int i = 0; do { // [RA]
              final String field = fields.get(i);
              if (field != null)
                builder.append("private=").append(fieldToString(field)).append(',');
            }
            while (++i < i$);
          }
          else {
            final Iterator<String> it = fields.iterator(); do { // [I]
              final String field = it.next();
              if (field != null)
                builder.append("private=").append(fieldToString(field)).append(',');
            }
            while (it.hasNext());
          }
        }
        else if (cacheControl.isPrivate()) {
          builder.append("private,");
        }
        else if (!(cacheControl instanceof StrictCacheControl) || ((StrictCacheControl)cacheControl).isPrivate != null) {
          builder.append("public,");
        }
      }
    },
    NO_CACHE("no-cache") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        final String field = parseFieldValue(directive);
        if (field == null)
          cacheControl.setNoCache(true);
        else
          cacheControl.getNoCacheFields().add(field);

        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        final List<String> fields = cacheControl.getNoCacheFields();
        final int i$ = fields.size();
        if (i$ > 0) {
          if (CollectionUtil.isRandomAccess(fields)) {
            int i = 0; do { // [RA]
              final String field = fields.get(i);
              if (field != null)
                builder.append("no-cache=").append(fieldToString(field)).append(',');
            }
            while (++i < i$);
          }
          else {
            final Iterator<String> it = fields.iterator(); do { // [I]
              final String field = it.next();
              if (field != null)
                builder.append("no-cache=").append(fieldToString(field)).append(',');
            }
            while (it.hasNext());
          }
        }
        else if (cacheControl.isNoCache()) {
          builder.append("no-cache,");
        }
      }
    },
    NO_STORE("no-store") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        cacheControl.setNoStore(true);
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.isNoStore())
          builder.append("no-store,");
      }
    },
    NO_TRANSFORM("no-transform") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        cacheControl.setNoTransform(true);
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.isNoTransform() && (!(cacheControl instanceof StrictCacheControl) || ((StrictCacheControl)cacheControl).noTransform != null))
          builder.append("no-transform,");
      }
    },
    MAX_AGE("max-age") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        final int index = directive.indexOf('=');
        if (index == -1)
          throw new IllegalArgumentException(directive);

        cacheControl.setMaxAge(Integer.parseInt(directive.substring(index + 1).trim()));
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.getMaxAge() == Integer.MAX_VALUE)
          builder.append("max-age,");
        else if (cacheControl.getMaxAge() != -1)
          builder.append("max-age=").append(cacheControl.getMaxAge()).append(',');
      }
    },
    S_MAX_AGE("s-maxage") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        final int index = directive.indexOf('=');
        if (index == -1)
          throw new IllegalArgumentException(directive);

        cacheControl.setSMaxAge(Integer.parseInt(directive.substring(index + 1).trim()));
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.getSMaxAge() == Integer.MAX_VALUE)
          builder.append("s-maxage,");
        else if (cacheControl.getSMaxAge() != -1)
          builder.append("s-maxage=").append(cacheControl.getMaxAge()).append(',');
      }
    },
    MUST_REVALIDATE("must-revalidate") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        cacheControl.setMustRevalidate(true);
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.isMustRevalidate())
          builder.append("must-revalidate,");
      }
    },
    PROXY_REVALIDATE("proxy-revalidate") {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        cacheControl.setProxyRevalidate(true);
        return true;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        if (cacheControl.isProxyRevalidate())
          builder.append("proxy-revalidate,");
      }
    },
    EXTENSION() {
      @Override
      boolean parse(final CacheControl cacheControl, final String directive) {
        final int index = directive.indexOf('=');
        if (index == -1)
          cacheControl.getCacheExtension().put(directive, null);
        else {
          final String key = directive.substring(0, index).trim();
          key.chars().forEach(c -> {
            if (c != '-' && !Character.isLetterOrDigit(c))
              throw new IllegalArgumentException(key);

          });
          cacheControl.getCacheExtension().put(key, directive.substring(index + 1).trim());
        }

        return false;
      }

      @Override
      void toString(final CacheControl cacheControl, final StringBuilder builder) {
        final Map<String,String> cacheExtension = cacheControl.getCacheExtension();
        if (cacheExtension.size() > 0) {
          for (final Map.Entry<String,String> entry : cacheExtension.entrySet()) { // [S]
            builder.append(entry.getKey());
            if (entry.getValue() != null)
              builder.append('=').append(entry.getValue());

            builder.append(',');
          }
        }
      }
    };

    private final String[] names;

    Directive(final String ... names) {
      this.names = names;
    }

    abstract boolean parse(CacheControl cacheControl, String value);
    abstract void toString(CacheControl cacheControl, StringBuilder builder);

    private static String fieldToString(String field) {
      return (field = field.trim()).length() > 0 ? "\"" + field + "\"" : null;
    }

    static boolean parseDirective(final StrictCacheControl cacheControl, final String directiveString) {
      final Directive[] directives = values();
      final int len = directives.length - 1;
      for (final Directive directive : directives) // [A]
        for (final String name : directive.names) // [A]
          if (Strings.startsWithIgnoreCase(directiveString, name) && (name.length() == directiveString.length() || directiveString.charAt(name.length()) == '='))
            return directive.parse(cacheControl, directiveString);

      return directives[len].parse(cacheControl, directiveString);
    }
  }

  static StrictCacheControl parse(final String value) {
    final String[] directives = Strings.split(value, ',');
    for (int i = 0, i$ = directives.length; i < i$; ++i) // [A]
      directives[i] = directives[i].trim();

    boolean valid = false;
    final StrictCacheControl cacheControl = new StrictCacheControl();
    for (int i = 0, i$ = directives.length; i < i$; ++i) // [A]
      valid |= Directive.parseDirective(cacheControl, directives[i]);

    if (!valid)
      throw new IllegalArgumentException(value);

    return cacheControl;
  }

  private static String parseFieldValue(final String value) {
    final int index = value.indexOf('=');
    if (index == -1)
      return null;

    final String field = value.substring(index + 1).trim();
    return field.startsWith("\"") && field.endsWith("\"") ? field.substring(1, field.length() - 1).trim() : field;
  }

  final DirectiveList<Directive> order = new DirectiveList<>();

  @Override
  public void setMustRevalidate(final boolean mustRevalidate) {
    super.setMustRevalidate(mustRevalidate);
    order.add(Directive.MUST_REVALIDATE);
  }

  @Override
  public void setProxyRevalidate(final boolean proxyRevalidate) {
    super.setProxyRevalidate(proxyRevalidate);
    order.add(Directive.PROXY_REVALIDATE);
  }

  @Override
  public void setMaxAge(final int maxAge) {
    super.setMaxAge(maxAge);
    order.add(Directive.MAX_AGE);
  }

  @Override
  public void setSMaxAge(final int sMaxAge) {
    super.setSMaxAge(sMaxAge);
    order.add(Directive.S_MAX_AGE);
  }

  @Override
  public void setNoCache(final boolean noCache) {
    super.setNoCache(noCache);
    order.add(Directive.NO_CACHE);
  }

  private Boolean isPrivate;

  @Override
  public void setPrivate(final boolean flag) {
    super.setPrivate(flag);
    this.isPrivate = flag;
    order.add(Directive.PRIVATE);
  }

  @Override
  public void setNoStore(final boolean noStore) {
    super.setNoStore(noStore);
    order.add(Directive.NO_STORE);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  private Boolean noTransform;

  @Override
  public void setNoTransform(final boolean noTransform) {
    super.setNoTransform(noTransform);
    this.noTransform = noTransform;
    order.add(Directive.NO_TRANSFORM);
  }
}