package org.bufio;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>{@code
 *     CsvReader r;
 *     while (r.next()) {
 *       String value1 = r.getString(1);
 *       // ...
 *     }
 * }</pre>
 */
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
		this.columnIndexes = columnIndexes; // TODO clone/copy
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
		columnIndexes = new HashMap<String, Integer>(n);
		for (int j = 0; j < n; j++) {
			columnIndexes.put(row[j], j + 1);
		}
	}

	/** @see java.sql.ResultSet#next */
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
	 * @see java.sql.ResultSet#getString(int)
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
	/** @see java.sql.ResultSet#getString(String) */
	public String getString(String columnLabel) throws IOException {
		return getString(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getByte(int)
	 */
	public byte getByte(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Byte.parseByte(value);
	}
	/** @see java.sql.ResultSet#getByte(String) */
	public byte getByte(String columnLabel) throws IOException {
		return getByte(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getShort(int)
	 */
	public short getShort(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Short.parseShort(value);
	}
	/** @see java.sql.ResultSet#getShort(String) */
	public short getShort(String columnLabel) throws IOException {
		return getShort(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getInt(int)
	 */
	public int getInt(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Integer.parseInt(value);
	}
	/** @see java.sql.ResultSet#getInt(String) */
	public int getInt(String columnLabel) throws IOException {
		return getInt(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getLong(int)
	 */
	public long getLong(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Long.parseLong(value);
	}
	/** @see java.sql.ResultSet#getLong(String) */
	public long getLong(String columnLabel) throws IOException {
		return getLong(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getFloat(int)
	 */
	public float getFloat(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Float.parseFloat(value);
	}
	/** @see java.sql.ResultSet#getFloat(String) */
	public float getFloat(String columnLabel) throws IOException {
		return getFloat(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getDouble(int)
	 */
	public double getDouble(int columnIndex) throws IOException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Double.parseDouble(value);
	}
	/** @see java.sql.ResultSet#getDouble(String) */
	public double getDouble(String columnLabel) throws IOException {
		return getDouble(findColumn(columnLabel));
	}

	/** @see java.sql.ResultSet#findColumn(String) */
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
	 * Returns current line number.
	 * @see java.sql.ResultSet#getRow
	 */
	public int getRow() { // FIXME row versus lineno
		return impl.lineno();
	}
	/** @see java.sql.ResultSetMetaData#getColumnCount() */
	public int getColumnCount() {
		return n;
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSetMetaData#getColumnLabel(int)
	 */
	public String getColumnLabel(int columnIndex) throws ScanException {
		if (columnIndexes == null || columnIndexes.isEmpty()) {
			throw new ScanException("No header");
		}
		if (columnIndex < 1) {
			throw new ScanException(String.format("Index out of bound (%d < 1)", columnIndex));
		}
		for (Map.Entry<String, Integer> entry : columnIndexes.entrySet()) {
			if (entry.getValue() == columnIndex) {
				return entry.getKey();
			}
		}
		throw new ScanException(String.format("No such index %d in %s", columnIndex, columnIndexes));
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
	/**
	 * Reports whether the last column read had an empty value.
	 * @see java.sql.ResultSet#wasNull
	 */
	public boolean wasNull() {
		return emptyIsNull ? wasNull : false;
	}

	@Override
	public void close() throws IOException {
		impl.close();
	}
}
