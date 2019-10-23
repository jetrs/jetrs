/* Copyright (c) 2019 OpenJAX
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

package org.jetrs.server.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jetrs.common.util.MediaTypes;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
@Consumes({"text/xml", "application/xml", "application/*+xml"})
@Produces({"text/xml", "application/xml", "application/*+xml"})
public class SourceProvider implements MessageBodyReader<Source>, MessageBodyWriter<Source> {
  private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return Source.class.isAssignableFrom(type) && (MediaTypes.TEXT_XML.isCompatible(mediaType) || MediaTypes.APPLICATION_XML.isCompatible(mediaType) || mediaType.getType().equals("application") && mediaType.getSubtype().endsWith("+xml"));
  }

  @Override
  public Source readFrom(final Class<Source> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException {
    return new StreamSource(entityStream);
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return isReadable(type, genericType, annotations, mediaType);
  }

  @Override
  public long getSize(final Source object, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final Source source, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException {
    try {
      transformerFactory.newTransformer().transform(source, new StreamResult(entityStream));
    }
    catch (final TransformerException e) {
      throw new IllegalStateException(e);
    }
  }
}