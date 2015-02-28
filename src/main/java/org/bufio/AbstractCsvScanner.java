package org.bufio;

import java.io.IOException;
import java.io.Reader;

// TODO lazy/strict

public abstract class AbstractCsvScanner<T> extends Scanner<T> {
  // values separator
  private final char sep; // withDelimiter
  // specify if values may be quoted (when they contain separator or newline)
  private final boolean quoted; // withQuote(char) + withEscape(char)
  // trim spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
  private boolean trim; // withIgnoreSurroundingSpaces
  // character marking the start of a line comment. When specified (not 0), line comments are ignored/skipped.
  private char comment; // withCommentMarker
  // ignore empty lines
  private boolean skipEmptyLines; // withIgnoreEmptyLines

  private int lineno;
  // true when the most recent field has been terminated by a newline (not a separator).
  private boolean eor;
  // current column (first column is 1)
  private int column;

  /**
   * Creates a "standard" CSV reader (separator is comma and quoted mode active)
   */
  protected AbstractCsvScanner(Reader r) {
    this(r, ',', true);
  }

  protected AbstractCsvScanner(Reader r, char sep, boolean quoted) {
    super(r);
    lineno = 1;
    eor = true;
    this.sep = sep;
    this.quoted = quoted;
    skipEmptyLines = true;
  }

  protected abstract T newToken(char[] data, int start, int end);

  /**
   * Returns current line number
   */
  public int lineno() {
    return lineno;
  }

  /**
   * Returns current column (first column is 1).
   */
  public int column() { return column; }

  /**
   * Returns `true` when the most recent field has been terminated by a newline (not a separator).
   */
  public boolean atEndOfRow() {
    return eor;
  }

  @Override
  protected T split(char[] data, int start, int end, boolean atEOF) throws ScanException {
    if (eor) {
      column = 1;
    } else {
      column++;
    }
    final T token = _split(data, start, end, atEOF);
    if (token == null) {
      if (column > 1) {
        column--;
      }
    }
    return token;
  }
  private T _split(char[] data, int start, int end, boolean atEOF) throws ScanException {
    if (atEOF && end == start) {
      if (eor) {
        return null;
      }
      eor = true;
      return newToken(data, start, end, false);
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
          throw new ScanException(String.format("unescaped %c character at line %d", pc, lineno));
        }
        ppc = pc;
        pc = c;
      }
      if (atEOF) {
        if (c == '"') {
          eor = true;
          advance(end);
          return unescapeQuotes(data, start + 1, end - 1, escapedQuotes);
        }
        // If we're at EOF, we have a non-terminated field.
        throw new ScanException(String.format("non-terminated quoted field at line %d", startLineno));
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

  /**
   * Skips `n` rows
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
   * Sets the character marking the start of a line comment. When specified (not 0), line comment appears as empty line.
   */
  public void setCommentMarker(char comment) {
    this.comment = comment;
  }

  /**
   * Trims spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
   */
  public void setTrim(boolean trim) {
    this.trim = trim;
  }

  public void setSkipEmptyLines(boolean skipEmptyLines) {
    this.skipEmptyLines = skipEmptyLines;
  }

  private T unescapeQuotes(char[] data, int start, int end, int count) {
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

  private T newToken(char[] data, int start, int end, boolean quoted) {
    if (trim && !quoted) {
      while (end > start && Character.isWhitespace(data[end - 1])) {
        end--;
      }
      while (start < end && Character.isWhitespace(data[start])) {
        start++;
      }
    }
    if (start == end && !quoted && column == 1 && eor && skipEmptyLines) {
      return null;
    }
    return newToken(data, start, end);
  }
}
