package org.safris.xrs.server.core;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.NioCompletionHandler;
import javax.ws.rs.core.NioErrorHandler;
import javax.ws.rs.core.NioReaderHandler;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

public class RequestImpl implements Request {
  private final String method;

  public RequestImpl(final String method) {
    this.method = method;
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public Variant selectVariant(final List<Variant> variants) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final EntityTag eTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final Date lastModified) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final Date lastModified, final EntityTag eTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void entity(final NioReaderHandler reader) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void entity(final NioReaderHandler reader, final NioCompletionHandler completion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void entity(final NioReaderHandler reader, final NioErrorHandler error) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void entity(final NioReaderHandler reader, final NioCompletionHandler completion, NioErrorHandler error) {
    throw new UnsupportedOperationException();
  }
}