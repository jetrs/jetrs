package org.safris.xrs.xjb;

public class GeneratorExecutionException extends Exception {
  private static final long serialVersionUID = 64500745605873916L;

  public GeneratorExecutionException() {
    super();
  }

  public GeneratorExecutionException(final String message) {
    super(message);
  }

  public GeneratorExecutionException(final Throwable cause) {
    super(cause);
  }

  public GeneratorExecutionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}