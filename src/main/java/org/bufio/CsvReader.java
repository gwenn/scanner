package org.bufio;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides an interface for reading CSV data by row
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * <pre>{@code
 * CsvReader r;
 * while (r.next()) {
 *   String value1 = r.getString(1);
 *   // ...
 * }
 * }</pre>
 */
public class CsvReader implements Closeable, Iterable<String[]> {
	private final CsvScanner impl;
	private String[] row = new String[10]; // row
	private int n; // number of field in current row
	private Map<String, Integer> columnIndexes;
	private Unmarshaler unmarshaler;

	private boolean emptyIsNull;
	private Boolean wasNull;

	/**
	 * Creates a "standard" CSV reader (separator is comma and quoted mode active)
	 */
	public CsvReader(@WillCloseWhenClosed @Nonnull Reader r) {
		impl = new CsvScanner(r);
	}

	/**
	 * Returns a new CSV scanner to read from `r`.
	 * When `quoted` is false, values must not contain a separator or newline.
	 */
	public CsvReader(@WillCloseWhenClosed @Nonnull Reader r, char sep, boolean quoted) {
		impl = new CsvScanner(r, sep, quoted);
	}

	/** Reuse this component with a new content. */
	public final void reset(@WillCloseWhenClosed @Nonnull Reader r) throws IOException {
		impl.reset(r);
		n = 0;
		columnIndexes = null; // TODO validate
		wasNull = null;
	}

	public void withHeaders(@Nonnull Iterable<String> headers) {
		columnIndexes = toColumnIndexes(headers);
	}

	@Nonnull
	public Map<String,Integer> scanHeaders(boolean ignoreCommentMarker) throws IOException {
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
			return Collections.emptyMap();
		}
		columnIndexes = new HashMap<>(n);
		for (int j = 0; j < n; j++) {
			columnIndexes.put(row[j], j + 1);
		}
		return columnIndexes; // TODO clone/copy
	}

	/** @see java.sql.ResultSet#next */
	public boolean next() throws IOException {
		scanRow();
		return n != 0;
	}

	private void scanRow() throws IOException {
		for (n = 0; impl.scan(); n++) {
			if (n == row.length) {
				row = Arrays.copyOf(row, n * 2); // FIXME limit
			}
			row[n] = impl.token();
			if (impl.atEndOfRow()) {
				n++;
				break;
			}
		}
		wasNull = null;
	}

	@Nonnull
	public String[] values() {
		return Arrays.copyOf(row, n);
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getString(int)
	 */
	@Nullable
	public String getString(@Nonnegative int columnIndex) throws ScanException {
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
	public String getString(String columnLabel) throws ScanException {
		return getString(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getByte(int)
	 */
	public byte getByte(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Byte.parseByte(value);
	}
	/** @see java.sql.ResultSet#getByte(String) */
	public byte getByte(String columnLabel) throws ScanException {
		return getByte(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getShort(int)
	 */
	public short getShort(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Short.parseShort(value);
	}
	/** @see java.sql.ResultSet#getShort(String) */
	public short getShort(String columnLabel) throws ScanException {
		return getShort(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getInt(int)
	 */
	public int getInt(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Integer.parseInt(value);
	}
	/** @see java.sql.ResultSet#getInt(String) */
	public int getInt(String columnLabel) throws ScanException {
		return getInt(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getLong(int)
	 */
	public long getLong(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Long.parseLong(value);
	}
	/** @see java.sql.ResultSet#getLong(String) */
	public long getLong(String columnLabel) throws ScanException {
		return getLong(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getFloat(int)
	 */
	public float getFloat(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Float.parseFloat(value);
	}
	/** @see java.sql.ResultSet#getFloat(String) */
	public float getFloat(String columnLabel) throws ScanException {
		return getFloat(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getDouble(int)
	 */
	public double getDouble(@Nonnegative int columnIndex) throws ScanException {
		final String value = getString(columnIndex);
		if (emptyIsNull && wasNull) {
			return 0;
		}
		return Double.parseDouble(value);
	}
	/** @see java.sql.ResultSet#getDouble(String) */
	public double getDouble(String columnLabel) throws ScanException {
		return getDouble(findColumn(columnLabel));
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSet#getObject(int, Class) */
	public <T> T getObject(@Nonnegative int columnIndex, Class<T> type) throws ScanException {
		if (unmarshaler == null) {
			throw new IllegalStateException("No unmarshaler set");
		}
		return unmarshaler.unmarshal(getString(columnIndex), type);
	}

	/** @see java.sql.ResultSet#getObject(String, Class) */
	public <T> T getObject(String columnLabel, Class<T> type) throws ScanException {
		return getObject(findColumn(columnLabel), type);
	}

	/** @see java.sql.ResultSet#findColumn(String) */
	public int findColumn(String columnLabel) throws ScanException {
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
	public void skipRows(@Nonnegative int n) throws IOException {
		impl.skipRows(n);
	}

	/**
	 * Returns current line number.
	 * @see java.sql.ResultSet#getRow
	 */
	@Nonnegative
	public int getRow() { // FIXME row versus lineno
		return impl.lineno();
	}
	/** @see java.sql.ResultSetMetaData#getColumnCount() */
	@Nonnegative
	public int getColumnCount() {
		return n;
	}

	/**
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @see java.sql.ResultSetMetaData#getColumnLabel(int)
	 */
	public String getColumnLabel(@Nonnegative int columnIndex) throws ScanException {
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

	/** Sets the component used by {@link #getObject} to transform text to object. */
	public void setUnmarshaler(@Nullable Unmarshaler unmarshaler) {
		this.unmarshaler = unmarshaler;
	}

	@Override
	public void close() throws IOException {
		impl.close();
	}

	/**
	 * Iterates on <i>rows</i>.
	 * <pre>{@code
	 * CsvReader r;
	 * for (String[] row : r) {
	 *   // ...
	 * }
	 * }</pre>
	 * @return an iterator on <i>rows</i>.
	 * @throws IllegalStateException for IOException.
	 */
	@Override
	@Nonnull
	public Iterator<String[]> iterator() {
		return new Iterator<String[]>() {
			private State state = State.NOT_READY;
			@Override
			public boolean hasNext() {
				if (State.FAILED == state) {
					throw new IllegalStateException();
				}
				if (State.DONE == state) {
					return false;
				} else if (State.READY == state) {
					return true;
				}
				state = State.FAILED;
				try {
					if (CsvReader.this.next()) {
						state = State.READY;
						return true;
					} else {
						state = State.DONE;
						return false;
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e); // TODO https://projectlombok.org/features/SneakyThrows.html
				}
			}
			@Override
			public String[] next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				state = State.NOT_READY;
				return CsvReader.this.values();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	private enum State {
		READY, NOT_READY, DONE, FAILED,
	}

	/**
	 * @return a sequential {@code Stream} over the elements in this reader.
	 */
	@Nonnull
	public Stream<String[]> stream() {
		return StreamSupport.stream(new CsvSpliterator(this), false);
	}
	private static class CsvSpliterator implements Spliterator<String[]> {
		private final CsvReader reader;

		private CsvSpliterator(CsvReader reader) {
			this.reader = reader;
		}

		@Override
		public boolean tryAdvance(Consumer<? super String[]> action) {
			try {
				if (reader.next()) {
					action.accept(reader.values());
					return true;
				}
				return false;
			} catch (IOException e) {
				throw new UncheckedIOException(e); // TODO https://projectlombok.org/features/SneakyThrows.html
			}
		}

		@Override
		public Spliterator<String[]> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE;
		}
	}

	static Map<String, Integer> toColumnIndexes(Iterable<String> headers) {
		final Map<String, Integer> columnIndexes = new HashMap<>();
		int i = 1;
		for (String header : headers) {
			columnIndexes.put(header, i++);
		}
		return columnIndexes;
	}

}
