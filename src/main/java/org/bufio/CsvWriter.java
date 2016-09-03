package org.bufio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * CsvWriter provides an interface for writing CSV data in accordance with the columns order
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * Successive calls to the `write` method will automatically insert the separator.
 * The `endOfRow` method tells when a line break is inserted.
 * <pre>{@code
 * CsvWriter w;
 * CsvReader r;
 * while (r.next()) {
 *   w.writeRow(r.values());
 * }
 * w.flush();
 * }</pre>
 */
public class CsvWriter implements Closeable, Flushable {
	private final Writer w;
	// values separator
	private final char sep;
	// specify if values should be quoted (when they contain a separator or a newline)
	private final boolean quoted;
	// True to use \r\n as the line terminator
	private boolean useCRLF;
	// character marking the start of a line comment.
	private char comment;
	private Marshaler marshaler;

	// true at start of row
	private boolean sor;
	private char[] buf;

	/** Creates a "standard" CSV writer (separator is comma and quoted mode active) */
	public CsvWriter(@WillCloseWhenClosed @Nonnull Writer w) {
		this(w, ',', true);
	}

	/** Returns a new CSV writer */
	public CsvWriter(@WillCloseWhenClosed @Nonnull Writer w, char sep, boolean quoted) {
		if (w == null) {
			throw new IllegalArgumentException("null writer");
		}
		this.w = w;
		this.sep = sep;
		this.quoted = quoted;
		sor = true;
		buf = new char[4096];
	}

	// Exports result to CSV.
	public void writeResultSet(@Nonnull ResultSet rs/*TODO, String nullValue*/, boolean headers) throws IOException, SQLException {
		final ResultSetMetaData metaData = rs.getMetaData();
		final int nCol = metaData.getColumnCount();
		if (headers) {
			for (int i = 1; i <= nCol; i++) {
				if (i == 1 && comment != 0) {
					write(comment + metaData.getColumnLabel(i));
					continue;
				}
				write(metaData.getColumnLabel(i));
			}
			endOfRow();
		}
		while (rs.next()) {
			for (int i = 1; i <= nCol; i++) {
				writeValue(rs.getObject(i));
			}
			endOfRow();
		}
	}

	public void writeRow(String... values) throws IOException {
		for (String value : values) {
			write(value);
		}
		endOfRow();
	}
	public void writeRow(@Nonnull Iterable<?> values) throws IOException {
		for (Object value : values) {
			writeValue(value);
		}
		endOfRow();
	}

	public void writeComment(String... values) throws IOException { // TODO only \r \n should be rejected
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

	// Value's type is used to encode value to text.
	public void writeValue(@Nullable Object value) throws IOException {
		if (marshaler != null) {
			write(marshaler.marshal(value));
		} else if (value == null) {
			write(buf, 0, 0);
		} else if (value instanceof String) {
			write((String) value);
		} else if (value instanceof Number) {
			write(value.toString());
		} else if (value instanceof Boolean) {
			write(value.toString()); // TODO parameterizable ("true"|"false")
		} else if (value instanceof Character) {
			buf[0] = (Character)value;
			write(buf, 0, 1);
		} else if (value instanceof char[]) {
			write((char[]) value);
		} else {
			throw new IllegalArgumentException("unsupported type: " + value.getClass());
		}
	}

	/** Ensures that value is quoted when needed. */
	public void write(@Nonnull char[] value) throws IOException {
		write(value, 0, value.length);
	}

	/** Ensures that value is quoted when needed. */
	public void write(@Nonnull String value) throws IOException {
		write(value, 0, value.length());
	}

	public void write(@Nonnull String str, int off, int len) throws IOException {
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

	/** Tells when a line break must be inserted.
	 * <pre>{@code
	 * CsvWriter w;
	 * CsvScanner s;
	 * while (s.scan())) {
	 *   String value = s.value();
	 *   w.write(value);
	 *   if (s.atEndOfRow()) {
	 *     w.endOfRow();
	 *   }
	 * }
	 * w.flush();
	 * }</pre>
	 */
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

	/** Sets the component called by {@link #writeValue} to marshall value to text. */
	public void setMarshaler(@Nullable Marshaler marshaler) {
		this.marshaler = marshaler;
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
