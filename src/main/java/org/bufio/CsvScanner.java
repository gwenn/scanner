package org.bufio;

import java.io.IOException;
import java.io.Reader;

/**
 * Provides an interface for reading CSV data
 * (compatible with rfc4180 and extended with the option of having a separator other than ",").
 * Successive calls to the `scan` method will step through the 'fields', skipping the separator/newline between the fields.
 * The `atEndOfRow` method tells when a field is terminated by a line break.
 * <pre>{@code
 * CsvScanner s;
 * while (s.scan())) {
 *   String value = s.value();
 *   // ...
 *   if (s.atEndOfRow()) {
 *     // ...
 *   }
 * }
 * }</pre>
 */
public class CsvScanner extends AbstractCsvScanner<String> {
	/**
	 * Creates a "standard" CSV reader (separator is comma and quoted mode active)
	 */
	public CsvScanner(Reader r) {
		super(r);
	}

	/**
	 * Returns a new CSV scanner to read from `r`.
	 * When `quoted` is false, values must not contain a separator or newline.
	 */
	public CsvScanner(Reader r, char sep, boolean quoted) {
		super(r, sep, quoted);
	}

	@Override
	protected String newToken(char[] data, int start, int end) {
		if (start == end) {
			return "";
		}
		return new String(data, start, end - start);
	}

	/**
	 * Extra fields are skipped (when the number of fields is greater than `values` size).
	 * Returns the number of values read (see #column()).
	 * At EOF, returns 0.
	 * <pre>{@code
	 * CsvScanner s;
	 * String[] values = new String[20]; // max columns
	 * int n;
	 * while ((n = s.scanRow(values)) > 0) {
	 *   String[] row = Arrays.copyOf(values, n);
	 *   // ...
	 * }
	 * }</pre>
	 */
	public int scanRow(String[] values) throws IOException {
		int i;
		for (i = 0; i < values.length && scan(); i++) {
			values[i] = value();
			if (atEndOfRow()) {
				return i + 1;
			}
		}
		// Extra values are skipped.
		//noinspection StatementWithEmptyBody
		while (!atEndOfRow() && scan()) {
		}
		return i;
	}

	/** Returns the most recent value generated by a call to scan. */
	public String value() {
		return token();
	}

	/** Reads text until next separator or eol/eof. */
	public String scanText() throws IOException {
		if (scan()) {
			return value();
		}
		return null;
	}
	/** Reads double until next separator or eol/eof. */
	public double scanDouble() throws IOException, IllegalArgumentException {
		return Double.parseDouble(scanText());
	}
	/** Reads float until next separator or eol/eof. */
	public float scanFloat() throws IOException, IllegalArgumentException {
		return Float.parseFloat(scanText());
	}
	/** Reads int until next separator or eol/eof. */
	public int scanInt() throws IOException, IllegalArgumentException {
		return Integer.parseInt(scanText());
	}
	/** Reads long until next separator or eol/eof. */
	public long scanLong() throws IOException, IllegalArgumentException {
		return Long.parseLong(scanText());
	}

	/** Reads bool until next separator or eol/eof. */
	public boolean scanBool(String trueValue) throws IOException {
		return trueValue.equals(scanText());
	}
	/** Reads char until next separator or eol/eof. */
	public char scanChar() throws IOException, IllegalArgumentException {
		final String text = scanText();
		if (text.length() == 1) {
			return text.charAt(0);
		}
		throw new IllegalArgumentException(String.format("expected character but got '%s'", text));
	}
}
