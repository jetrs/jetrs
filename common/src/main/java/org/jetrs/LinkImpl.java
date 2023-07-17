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

import static org.libj.lang.Assertions.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.libj.lang.Strings;

public class LinkImpl extends Link {
  static class BuilderImpl implements Link.Builder {
    protected final HashMap<String,String> params = new HashMap<>();
    protected UriBuilder uriBuilder;
    protected URI baseUri;

    @Override
    public Link.Builder baseUri(final String uri) {
      this.baseUri = URI.create(uri);
      return this;
    }

    @Override
    public Link.Builder baseUri(final URI uri) {
      this.baseUri = uri;
      return this;
    }

    @Override
    public Link build(final Object ... values) throws UriBuilderException {
      URI uri = uriBuilder == null ? baseUri : uriBuilder.build(values);

      if (!uri.isAbsolute() && baseUri != null)
        uri = baseUri.resolve(uri);

      return new LinkImpl(uri, params);
    }

    @Override
    public Link buildRelativized(final URI uri, final Object ... values) {
      final URI built = uriBuilder.build(values);
      final URI with = baseUri != null ? baseUri.resolve(built) : built;
      return new LinkImpl(uri.relativize(with), params);
    }

    @Override
    public Link.Builder link(final Link link) {
      uriBuilder = UriBuilder.fromUri(link.getUri());
      params.clear();
      params.putAll(link.getParams());
      return this;
    }

    @Override
    public Link.Builder link(final String link) {
      return link(Links.parse(link));
    }

    @Override
    public Link.Builder param(final String name, final String value) throws IllegalArgumentException {
      params.put(assertNotNull(name), assertNotNull(value));
      return this;
    }

    @Override
    public Link.Builder rel(final String rel) {
      final String rels = params.get(Link.REL);
      param(Link.REL, rels != null ? rels + " " + assertNotNull(rel) : rel);
      return this;
    }

    @Override
    public Link.Builder title(final String title) {
      param(Link.TITLE, title);
      return this;

    }

    @Override
    public Link.Builder type(final String type) {
      param(Link.TYPE, type);
      return this;
    }

    @Override
    public Link.Builder uri(final String uri) throws IllegalArgumentException {
      uriBuilder = UriBuilder.fromUri(uri);
      return this;
    }

    @Override
    public Link.Builder uri(final URI uri) {
      uriBuilder = UriBuilder.fromUri(uri);
      return this;
    }

    @Override
    public Link.Builder uriBuilder(final UriBuilder uriBuilder) {
      this.uriBuilder = uriBuilder.clone();
      return this;
    }
  }

  protected final URI uri;
  protected final Map<String,String> params;

  LinkImpl(final URI uri, final Map<String,String> params) {
    this.uri = Objects.requireNonNull(uri);
    this.params = params.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(params));
  }

  LinkImpl(final URI uri, final String relation) {
    this.uri = Objects.requireNonNull(uri);
    this.params = Collections.singletonMap(REL, Objects.requireNonNull(relation));
  }

  @Override
  public Map<String,String> getParams() {
    return params;
  }

  @Override
  public String getRel() {
    return params.get(REL);
  }

  @Override
  public List<String> getRels() {
    final String rels = params.get(REL);
    return rels == null ? Collections.emptyList() : Arrays.asList(Strings.split(rels, '\n'));
  }

  @Override
  public String getTitle() {
    return params.get(TITLE);
  }

  @Override
  public String getType() {
    return params.get(TYPE);
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public UriBuilder getUriBuilder() {
    return UriBuilder.fromUri(uri);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof LinkImpl))
      return false;

    final LinkImpl that = (LinkImpl)obj;
    return uri.equals(that.uri) && params.equals(that.params);
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    hashCode = 31 * hashCode + uri.hashCode();
    hashCode = 31 * hashCode + params.hashCode();
    return hashCode;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append('<').append(uri).append('>');
    if (params.size() > 0) {
      for (final Map.Entry<String,String> entry : params.entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        if (REL.equals(key)) {
          for (final String rel : Strings.split(value, '\n')) // [A]
            b.append("; ").append(key).append("=\"").append(rel).append('"');
        }
        else {
          b.append("; ").append(key).append("=\"").append(value).append('"');
        }
      }
    }

    return b.toString();
  }
}