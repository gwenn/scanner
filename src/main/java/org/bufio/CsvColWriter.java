package org.bufio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * CsvColWriter provides an interface for writing CSV data by row
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * Calls to the `setX` methods can be made in any order.
 * The `endOfRow` method tells when a line break is inserted.
 * <pre>{@code
 * CsvColWriter w;
 * CsvReader r;
 * w.withColumnIndexes(r.scanHeaders(true));
 * w.writerHeaders(false);
 * while (r.next()) {
 *   w.setString(2, r.getString(1));
 *   w.setString(1, r.getString(2));
 *   ...
 *   w.endOfRow();
 * }
 * w.flush();
 * }</pre>
 */
public class CsvColWriter implements Closeable, Flushable {
	private final CsvWriter writer;
	private Object[] row = new Object[10]; // row
	private int n; // number of field in current row
	private List<String> headers;
	private Map<String, Integer> columnIndexes;

	public CsvColWriter(Writer w) {
		writer = new CsvWriter(w);
	}

	public CsvColWriter(Writer w, char sep, boolean quoted) {
		writer = new CsvWriter(w, sep, quoted);
	}

	public void withHeaders(Collection<String> headers) {
		this.headers = new ArrayList<>(headers);
		this.columnIndexes = CsvReader.toColumnIndexes(headers);
	}

	/** @see java.sql.ResultSet#findColumn(String) */
	public int findColumn(String columnLabel) throws IllegalStateException {
		if (columnIndexes == null || columnIndexes.isEmpty()) {
			throw new IllegalStateException("No header");
		}
		final Integer columnIndex = columnIndexes.get(columnLabel);
		if (columnIndex == null) {
			throw new IllegalStateException(String.format("No such column '%s' in %s", columnLabel, columnIndexes.keySet()));
		}
		return columnIndex;
	}

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
	 * @throws IllegalStateException if no header has been specified ({@link #withHeaders}).
	 */
	public void writerHeaders(boolean useCommentMarker) throws IOException {
		if (columnIndexes == null || columnIndexes.isEmpty()) {
			throw new IllegalStateException("No header");
		}
		if (useCommentMarker) {
			writer.writeComment(headers.toArray(new String[headers.size()]));
		} else {
			writer.writeRow(headers);
		}
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setString(int columnIndex, String x) {
		setObject(columnIndex, x);
	}
	public void setString(String columnLabel, String x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setByte(int columnIndex, byte x) {
		setObject(columnIndex, x);
	}
	public void setByte(String columnLabel, byte x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setShort(int columnIndex, short x) {
		setObject(columnIndex, x);
	}
	public void setShort(String columnLabel, short x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setInt(int columnIndex, int x) {
		setObject(columnIndex, x);
	}
	public void setInt(String columnLabel, int x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setLong(int columnIndex, long x) {
		setObject(columnIndex, x);
	}
	public void setLong(String columnLabel, long x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setFloat(int columnIndex, float x) {
		setObject(columnIndex, x);
	}
	public void setFloat(String columnLabel, float x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setDouble(int columnIndex, double x) {
		setObject(columnIndex, x);
	}
	public void setDouble(String columnLabel, double x) {
		setObject(columnLabel, x);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @param x the column value
	 */
	public void setObject(int columnIndex, Object x) {
		final int i = columnIndex - 1;
		if (i == row.length) {
			row = Arrays.copyOf(row, i * 2); // FIXME limit
		}
		n = Math.max(n, columnIndex);
		row[i] = x;
	}
	public void setObject(String columnLabel, Object x) {
		setObject(findColumn(columnLabel), x);
	}

	/** Tells when a line break must be inserted. */
	public void endOfRow() throws IOException {
		for (int i = 0; i < n; i++) {
			writer.writeValue(row[i]);
		}
		writer.endOfRow();
		n = 0; // TODO ensure n is reset to 0 even if an IOException occurs...
	}

	/** Use \r\n as the line terminator. */
	public void useCRLF() {
		writer.useCRLF();
	}
	/** Sets the character marking the start of a line comment. */
	public void setCommentMarker(char comment) {
		writer.setCommentMarker(comment);
	}

	/** Sets the component called by {@link #setObject} to marshall value to text. */
	public void setMarshaler(Marshaler marshaler) {
		writer.setMarshaler(marshaler);
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
	@Override
	public void flush() throws IOException {
		writer.flush();
	}
}
