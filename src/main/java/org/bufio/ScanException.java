package org.bufio;

import java.io.IOException;

public class ScanException extends IOException {
  public ScanException(Throwable cause) {
    super(cause);
  }

  public ScanException(String message) {
    super(message);
  }

  public ScanException(String message, Throwable cause) {
    super(message, cause);
  }
}
