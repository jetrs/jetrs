/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.safris.commons.util.Collections;
import org.safris.xrs.server.util.MediaTypes;

public class EntityProviders {
  private static final Comparator<EntityProvider<?>> comparator = new Comparator<EntityProvider<?>>() {
    @Override
    public int compare(final EntityProvider<?> o1, final EntityProvider<?> o2) {
      final Class<?> c1 = o1.getProvider().getClass();
      final Class<?> c2 = o2.getProvider().getClass();
      return c1 == c2 ? 0 : c1.isAssignableFrom(c2) ? 1 : -1;
    }
  };

  private class ReaderProvider extends EntityProvider<MessageBodyReader<?>> {
    public ReaderProvider(final MessageBodyReader<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final MediaType mediaType, final Class<?> type) {
      return super.matches(mediaType, type) && getProvider().isReadable(type, type.getGenericSuperclass(), type.getAnnotations(), mediaType);
    }
  }

  private class WriterProvider extends EntityProvider<MessageBodyWriter<?>> {
    public WriterProvider(final MessageBodyWriter<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final MediaType mediaType, final Class<?> type) {
      return super.matches(mediaType, type) && getProvider().isWriteable(type, type.getGenericSuperclass(), type.getAnnotations(), mediaType);
    }
  }

  private abstract class EntityProvider<T> {
    private final MediaType[] allowedTypes;
    private final T provider;

    public EntityProvider(final T provider) {
      this.provider = provider;
      final Consumes consumes = provider.getClass().getAnnotation(Consumes.class);
      this.allowedTypes = consumes == null ? new MediaType[] {MediaType.WILDCARD_TYPE} : MediaTypes.parse(consumes.value());
    }

    public boolean matches(final MediaType mediaType, final Class<?> type) {
      return MediaTypes.matches(mediaType, allowedTypes);
    }

    public T getProvider() {
      return provider;
    }
  }

  private final List<ReaderProvider> readerProviders = new ArrayList<ReaderProvider>();
  private final List<WriterProvider> writerProviders = new ArrayList<WriterProvider>();

  public EntityProviders(final List<MessageBodyReader<?>> readerProviders, final List<MessageBodyWriter<?>> writerProviders) {
    for (final MessageBodyReader<?> readerProvider : readerProviders)
      this.readerProviders.add(new ReaderProvider(readerProvider));

    for (final MessageBodyWriter<?> writerProvider : writerProviders)
      this.writerProviders.add(new WriterProvider(writerProvider));

    Collections.sort(this.readerProviders, comparator);
    Collections.sort(this.writerProviders, comparator);
  }

  public MessageBodyWriter<?> getWriter(final MediaType mediaType, final Class<?> type) {
    for (final WriterProvider provider : writerProviders)
      if (provider.matches(mediaType, type))
        return provider.getProvider();

    return null;
  }

  public MessageBodyReader<?> getReader(final MediaType mediaType, final Class<?> type) {
    for (final ReaderProvider provider : readerProviders)
      if (provider.matches(mediaType, type))
        return provider.getProvider();

    return null;
  }
}