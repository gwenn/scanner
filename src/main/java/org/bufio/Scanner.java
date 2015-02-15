package org.bufio;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Port of Go Scanner in Java.
 */
public abstract class Scanner<T> implements Closeable {
  // The reader provided by the client.
  private final Reader r;
  // Maximum size of a token
  private int maxTokenSize;
  // Last token returned by split.
  private T token;
  // Buffer used as argument to split.
  private char[] buf;
  // First non-processed byte in buf.
  private int start;
  // End of data in buf.
  private int end;

  private boolean eof;

  protected Scanner(Reader r) {
    this.r = r;
    this.maxTokenSize = 64 * 1024;
    this.buf = new char[4096]; // Plausible starting size; needn't be large.
  }

  public boolean scan() throws IOException {
    // Loop until we have a token.
    while (true) {
      // See if we can get a token with what we already have.
      if (end > start || eof) {
        token = split(buf, start, end, eof);
        if (token != null) {
          return true;
        }
      }
      // We cannot generate a token with what we are holding.
      // If we've already hit EOF or an I/O error, we are done.
      if (eof) {
        start = 0;
        end = 0;
        return false;
      }
      read();
    }
  }

  private void read() throws IOException {
    // Must read more data.
    // First, shift data to beginning of buffer if there's lots of empty space
    // or space is needed.
    if (start > 0 && (end == buf.length || start > buf.length / 2)) {
      System.arraycopy(buf, start, buf, 0, end - start);
      end -= start;
      start = 0;
    }
    // Is the buffer full? If so, resize.
    if (end == buf.length) {
      if (buf.length >= maxTokenSize) {
        throw new IOException("token too long");
      }
      int newSize = Math.max(buf.length * 2, maxTokenSize);
      char[] newBuf = new char[newSize];
      System.arraycopy(buf, start, newBuf, 0, end - start);
      buf = newBuf;
      end -= start;
      start = 0;
      return;
    }
    // Finally we can read some input.
    int n = r.read(buf, end, buf.length - end);
    if (n < 0) {
      eof = true;
    } else {
      end += n;
    }
  }

  // The function to split the tokens.
  protected abstract T split(char[] data, int start, int end, boolean atEOF) throws IOException;

  protected T token() {
    return token;
  }

  protected char peek() throws IOException {
    while (true) {
      if (end > start) {
        return buf[start];
      }
      if (eof) {
        return 0;
      }
      read();
    }
  }

  @Override
  public void close() {
    if (r != null) {
      try {
        r.close();
      } catch (IOException ignored) {
      }
    }
  }

  protected void advance(int n) throws IOException {
    if (n < 0) {
      throw new IOException("SplitFunc returns negative advance count");
    }
    if (n > end) {
      throw new IOException("SplitFunc returns advance count beyond input");
    }
    start = n;
  }
}
