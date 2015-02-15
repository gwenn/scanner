package org.bufio;

import java.io.IOException;
import java.io.Reader;

// TODO lazy
// setSkipEmptyLines/ ignore empty lines
// TODO set the number of headers line to skip

/**
 * Provides an interface for reading CSV data
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * Successive calls to the `scan` method will step through the 'fields', skipping the separator/newline between the fields.
 * The `atEndOfRow` method tells when a field is terminated by a line break.
 * <p><blockquote><pre>
 *     CsvScanner s;
 *     while (s.scan())) {
 *       String value = s.token();
 *       // ...
 *       if (s.atEndOfRow()) {
 *         // ...
 *       }
 *     }
 * </pre></blockquote>
 * <p>
 */
public class CsvScanner extends Scanner<String> {
  // values separator
  private final char sep; // withDelimiter
  // specify if values may be quoted (when they contain separator or newline)
  private final boolean quoted; // withQuote(char) + withEscape(char)
  // trim spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
  private boolean trim; // withIgnoreSurroundingSpaces
  // character marking the start of a line comment. When specified, line comments are ignored/skipped.
  private char comment; // withCommentMarker

  private int lineno;
  // true when the most recent field has been terminated by a newline (not a separator).
  private boolean eor;

  /**
   * Create a "standard" CSV reader (separator is comma and quoted mode active)
   */
  public CsvScanner(Reader r) {
    this(r, ',', true);
  }

  /**
   * Return a new CSV scanner to read from `r`.
   * When `quoted` is false, values must not contain a separator or newline.
   */
  public CsvScanner(Reader r, char sep, boolean quoted) {
    super(r);
    this.sep = sep;
    this.quoted = quoted;
    eor = true;
    lineno = 1;
  }

  /**
   * Return current line number
   */
  public int lineno() {
    return lineno;
  }

  /**
   * Return `true` when the most recent field has been terminated by a newline (not a separator).
   */
  public boolean atEndOfRow() {
    return eor;
  }

  @Override
  protected String split(char[] data, int start, int end, boolean atEOF) throws IOException {
    if (atEOF && end == start) {
      if (eor) {
        return null;
      }
      eor = true;
      return "";
    }
    if (quoted && start < end && data[start] == '"') { // quoted field (may contains separator, newline and escaped quote)
      int startLineno = lineno;
      int escapedQuotes = 0;
      char c = 0, pc = 0, ppc = 0;
      // Scan until the separator or newline following the closing quote (and ignore escaped quote)
      for (int i = start + 1; i < end; i++) {
        c = data[i];
        if (c == '\n') {
          lineno++;
        } else if (c == '"') {
          if (pc == c) { // escaped quote
            pc = 0;
            escapedQuotes++;
            continue;
          }
        }
        if (pc == '"' && c == sep) {
          eor = false;
          advance(i + 1);
          return unescapeQuotes(data, start + 1, i - 1, escapedQuotes);
        } else if (pc == '"' && c == '\n') {
          eor = true;
          advance(i + 1);
          return unescapeQuotes(data, start + 1, i - 1, escapedQuotes);
        } else if (ppc == '"' && pc == '\r' && c == '\n') {
          eor = true;
          advance(i + 1);
          return unescapeQuotes(data, start + 1, i - 2, escapedQuotes);
        }
        if (pc == '"' && c != '\r') {
          throw new IOException(String.format("unescaped %c character at line %d", pc, lineno));
        }
        ppc = pc;
        pc = c;
      }
      if (atEOF) {
        if (c == '"') {
          eor = true;
          advance(end);
          return unescapeQuotes(data, start+1, end - 1, escapedQuotes);
        }
        // If we're at EOF, we have a non-terminated field.
        throw new IOException(String.format("non-terminated quoted field at line %d", startLineno));
      }
    } else if (eor && comment != 0 && start < end && data[start] == comment) { // line comment
      for (int i = start; i < end; i++) {
        if (data[i] == '\n') {
          lineno++;
          advance(i + 1);
          return null;
        }
      }
      if (atEOF) {
        advance(end);
        return null;
      }
    } else { // unquoted field
      // Scan until separator or newline, marking end of field.
      char c, pc = 0;
      for (int i = start; i < end; i++) {
        c = data[i];
        if (c == sep) {
          eor = false;
          advance(i + 1);
          return newToken(data, start, i, false);
        }
        if (c == '\n') {
          eor = true;
          lineno++;
          advance(i + 1);
          return newToken(data, start, pc == '\r' ? i - 1 : i, false);
        }
        pc = c;
      }
      // If we're at EOF, we have a final, non-terminated line. Return it.
      if (atEOF) {
        eor = true;
        advance(end);
        return newToken(data, start, pc == '\r' ? end - 1 : end, false);
      }
    }
    // Request more data.
    return null;
  }

  private String unescapeQuotes(char[] data, int start, int end, int count) {
    if (count == 0) {
      return newToken(data, start, end, true);
    }
    for (int i = start, j = start; i < end; i++, j++) {
      data[j] = data[i];
      if (data[i] == '"') {
        i++;
      }
    }
    return newToken(data, start, end - count, true);
  }

  private String newToken(char[] data, int start, int end, boolean quoted) {
    if (trim && !quoted) {
      while (end > start && Character.isWhitespace(data[end-1])) {
        end--;
      }
      while (start < end && Character.isWhitespace(data[start])) {
        start++;
      }
    }
    if (start == end) {
      return "";
    }
    return new String(data, start, end - start);
  }

  /**
   * Empty lines (or line comments) are skipped.
   * Extra fields are skipped (when the number of fields is greater than `values` size).
   * Returns the number of fields read (not the number of values).
   * At EOF, returns 0.
   * <p><blockquote><pre>
   *     CsvScanner s;
   *     String[] values = new String[20]; // max columns
   *     int n;
   *     while ((n = s.scanRow(values)) > 0) {
   *       String[] row = Arrays.copyOf(values, n);
   *       // ...
   *     }
   * </pre></blockquote>
   * <p>
   */
  public int scanRow(String[] values) throws IOException {
    int i;
    for (i = 0; i < values.length && scan(); i++) {
      if (i == 0) {
        while (eor && token().isEmpty()) { // skip empty line
          if (!scan()) {
            return i;
          }
        }
      }
      values[i] = token();
      if (eor) {
        return i + 1;
      }
    }
    // Extra values are skipped.
    while (!eor && scan()) {
      i++;
    }
    return i;
  }

  /**
   * Skip `n` rows
   */
  public void skipRows(int n) throws IOException {
    int i = 0;
    while (i < n && scan()) {
      if (eor) {
        i++;
      }
    }
  }

  /**
   * Set the character marking the start of a line comment. When specified, line comment appears as empty line.
   */
  public void setCommentMarker(char comment) {
    this.comment = comment;
  }

  /**
   * Trim spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
   */
  public void setTrim(boolean trim) {
    this.trim = trim;
  }
}
