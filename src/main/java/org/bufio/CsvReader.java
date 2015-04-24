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

  private boolean emptyIsNull;
  private Boolean wasNull;

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

  public void scanHeaders(boolean ignoreCommentMarker) throws IOException {
    char pcm = '\0';
    if (ignoreCommentMarker) {
      pcm = impl.setCommentMarker('\0');
    }
    try {
      scanRow();
    } finally {
      if (ignoreCommentMarker) {
        impl.setCommentMarker(pcm);
      }
    }
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
    wasNull = null;
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
    final String value = row[columnIndex - 1];
    if (emptyIsNull) {
      wasNull = value.isEmpty();
      if (wasNull) {
        return null;
      }
    }
    return value;
  }
  public String getString(String columnLabel) throws IOException {
    return getString(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public byte getByte(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Byte.parseByte(value);
  }
  public byte getByte(String columnLabel) throws IOException {
    return getByte(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public short getShort(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Short.parseShort(value);
  }
  public short getShort(String columnLabel) throws IOException {
    return getShort(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public int getInt(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Integer.parseInt(value);
  }
  public int getInt(String columnLabel) throws IOException {
    return getInt(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public long getLong(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Long.parseLong(value);
  }
  public long getLong(String columnLabel) throws IOException {
    return getLong(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public float getFloat(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Float.parseFloat(value);
  }
  public float getFloat(String columnLabel) throws IOException {
    return getFloat(findColumn(columnLabel));
  }

  /**
   * @param columnIndex the first column is 1, the second is 2, ...
   */
  public double getDouble(int columnIndex) throws IOException {
    final String value = getString(columnIndex);
    if (emptyIsNull && wasNull) {
      return 0;
    }
    return Double.parseDouble(value);
  }
  public double getDouble(String columnLabel) throws IOException {
    return getDouble(findColumn(columnLabel));
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

  /** Treats empty value as `null` value */
  public void setEmptyIsNull(boolean emptyIsNull) {
    this.emptyIsNull = emptyIsNull;
  }
  /** Reports whether the last column read had an empty value. */
  public boolean wasNull() {
    return wasNull;
  }

  @Override
  public void close() throws IOException {
    impl.close();
  }
}
