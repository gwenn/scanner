package org.bufio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

/**
 * CsvPrinter provides an interface for writing CSV data.
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * Successive calls to the `write` method will automatically insert the separator.
 * The `endOfRow` method tells when a line break is inserted.
 */
public class CsvWriter implements Closeable, Flushable {
  private final Writer w;
  // values separator
  private final char sep;
  // specify if values should be quoted (when they contain a separator or a newline)
  private final boolean quoted;
  // True to use \r\n as the line terminator
  private boolean useCRLF;
  // true at start of row
  private boolean sor;
  // character marking the start of a line comment.
  private char comment;

  private char[] buf;

  /** Creates a "standard" CSV writer (separator is comma and quoted mode active) */
  public CsvWriter(Writer w) {
    this(w, ',', true);
  }

  /** Returns a new CSV writer */
  public CsvWriter(Writer w, char sep, boolean quoted) {
    if (w == null) {
      throw new IllegalArgumentException("null writer");
    }
    this.w = w;
    this.sep = sep;
    this.quoted = quoted;
    sor = true;
    buf = new char[4096];
  }

  public void writeRow(String... values) throws IOException {
    for (String value : values) {
      write(value);
    }
    endOfRow();
  }

  public void writeComment(String... values) throws IOException {
    boolean first = true;
    for (String value : values) {
      if (first && comment != 0) {
        first = false;
        write(comment + value);
        continue;
      }
      write(value);
    }
    endOfRow();
  }

  /** Ensures that value is quoted when needed. */
  public void write(char[] value) throws IOException {
    write(value, 0, value.length);
  }

  /** Ensures that value is quoted when needed. */
  public void write(String value) throws IOException {
    write(value, 0, value.length());
  }

  public void write(String str, int off, int len) throws IOException {
    if (len > buf.length) { // FIXME
      buf = new char[len];
    }
    str.getChars(off, len, buf, 0);
    write(buf, 0, len);
  }

  /** Ensures that value is quoted when needed. */
  public void write(char[] data, int start, int end) throws IOException {
    if (!sor) {
      w.append(sep);
    }
    // In quoted mode, value is enclosed between quotes if it contains sep, quote or \n.
    if (quoted) {
      int last = start;
      for (int i = start; i < end; i++) {
        char c = data[i];
        if (c != '"' && c != '\r' && c != '\n' && c != sep) {
          continue;
        }
        if (last == start) {
          w.write('"');
        }
        w.write(data, last, i + 1 - last);
        if (c == '"') {
          w.write('"'); // escaped with another double quote
        }
        last = i + 1;
      }
      w.write(data, last, end - last);
      if (last != start) {
        w.write('"');
      }
    } else {
      // check that value does not contain sep or \n
      for (int i = start; i < end; i++) {
        char c = data[i];
        if (c == '\n') {
          throw new IOException("newline character in value");
        } else if (c == sep) {
          throw new IOException("separator in value");
        }
      }
      w.write(data, start, end - start);
    }
    sor = false;
  }

  /** Tells when a line break must be inserted. */
  public void endOfRow() throws IOException {
    if (useCRLF) {
      w.write('\r');
    }
    w.write('\n');
    sor = true;
  }

  /** Use \r\n as the line terminator. */
  public void useCRLF() {
    useCRLF = true;
  }
  /** Sets the character marking the start of a line comment. */
  public void setCommentMarker(char comment) {
    this.comment = comment;
  }

  @Override
  public void flush() throws IOException {
    w.flush();
  }

  @Override
  public void close() throws IOException {
    w.close();
  }
}
