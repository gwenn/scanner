package org.bufio;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CsvReader implements Closeable {
  private final CsvScanner impl;
  private String[] row = new String[10]; // row
  private int n; // number of field in current row
  private Map<String, Integer> columnIndexes;

  /**
   * Creates a "standard" CSV reader (separator is comma and quoted mode active)
   */
  public CsvReader(Reader r) {
    impl = new CsvScanner(r);
  }

  /**
   * Returns a new CSV scanner to read from `r`.
   * When `quoted` is false, values must not contain a separator or newline.
   */
  public CsvReader(Reader r, char sep, boolean quoted) {
    impl = new CsvScanner(r, sep, quoted);
  }

  /**
   * The first column is 1, the second is 2, ...
   */
  public void withColumnIndexes(Map<String, Integer> columnIndexes) {
    for (Map.Entry<String, Integer> entry : columnIndexes.entrySet()) {
      if (entry.getValue() == null) {
        throw new IllegalArgumentException(String.format("Null index for '%s'", entry.getKey()));
      } else if (entry.getValue() < 1) {
        throw new IllegalArgumentException(String.format("Invalid index for '%s': %d < 1", entry.getKey(), entry.getValue()));
      }
    }
    this.columnIndexes = columnIndexes;
  }

  public void scanHeaders() throws IOException {
    // TODO ignore comment marker
    scanRow();
    if (n == 0) {
      return;
    }
    columnIndexes = new HashMap<String,Integer>(n);
    for (int j = 0; j < n; j++) {
      columnIndexes.put(row[j], j+1);
    }
  }

  public boolean next() throws IOException {
    scanRow();
    return n != 0;
  }

  private void scanRow() throws IOException {
    for (n = 0; impl.scan(); n++) {
      if (n == row.length) {
        row = Arrays.copyOf(row, n * 2);
      }
      row[n] = impl.token();
      if (impl.atEndOfRow()) {
        n++;
        break;
      }
    }
  }

  public String[] values() {
    return Arrays.copyOf(row, n);
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public String getString(int columnIndex) throws IOException {
    if (n == 0) {
      throw new ScanException("No row");
    }
    if (columnIndex < 1) {
      throw new ScanException(String.format("Index out of bound (%d < 1)", columnIndex));
    }
    if (columnIndex > n) {
      throw new ScanException(String.format("Index out of bound (%d > %d)", columnIndex, n));
    }
    return row[columnIndex-1];
  }
  public String getString(String columnLabel) throws IOException {
    return getString(findColumn(columnLabel));
  }

  public int findColumn(String columnLabel) throws IOException {
    if (columnIndexes == null || columnIndexes.isEmpty()) {
      throw new ScanException("No header");
    }
    final Integer columnIndex = columnIndexes.get(columnLabel);
    if (columnIndex == null) {
      throw new ScanException(String.format("No such column '%s' in %s", columnLabel, columnIndexes.keySet()));
    }
    return columnIndex;
  }

  /**
   * Skips `n` rows
   */
  public void skipRows(int n) throws IOException {
    impl.skipRows(n);
  }

  /**
   * Returns current line number
   */
  public int lineno() {
    return impl.lineno();
  }

  public int getColumnCount() {
    return n;
  }

  /**
   * @param column the first column is 1, the second is 2, ...
   */
  public String getColumnLabel(int column) throws ScanException {
    if (columnIndexes == null || columnIndexes.isEmpty()) {
      throw new ScanException("No header");
    }
    for (Map.Entry<String, Integer> entry : columnIndexes.entrySet()) {
      if (entry.getValue() == column) {
        return entry.getKey();
      }
    }
    throw new ScanException(String.format("No such column '%d' in %s", column, columnIndexes));
  }

  /**
   * Sets the character marking the start of a line comment. When specified (not 0), line comment appears as empty line.
   */
  public void setCommentMarker(char comment) {
    impl.setCommentMarker(comment);
  }

  /**
   * Trims spaces (only on not-quoted values). Break rfc4180 rule: "Spaces are considered part of a field and should not be ignored."
   */
  public void setTrim(boolean trim) {
    impl.setTrim(trim);
  }

  public void setSkipEmptyLines(boolean skipEmptyLines) {
    impl.setSkipEmptyLines(skipEmptyLines);
  }

  @Override
  public void close() throws IOException {
    impl.close();
  }
}
