package org.bufio;

import org.junit.Test;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CsvScannerTest {
	@Test
	public void testScan() throws IOException {
		CsvScanner r;
		for (ReadTest t : ReadTest.tests) {
			r = new CsvScanner(new StringReader(t.input), t.sep, t.quoted);
			r.setCommentMarker(t.comment);
			r.setTrim(t.trim);
			r.setSkipEmptyLines(t.skipEmptyLines);

			int i = 0, j = 0;
			try {
				while (r.scan()) {
					if (i >= t.output.length) {
						fail(String.format("%s: unexpected number of row %d; want %d max", t.name, i + 1, t.output.length));
					} else if (j >= t.output[i].length) {
						fail(String.format("%s: unexpected number of column %d; want %d at line %d", t.name, j + 1, t.output[i].length, i + 1));
					}
					if (!t.output[i][j].equals(r.token())) {
						fail(String.format("%s: unexpected value '%s'; want '%s' at line %d, column %d", t.name, r.token(), t.output[i][j], i + 1, j + 1));
					}
					if (r.atEndOfRow()) {
						j = 0;
						i++;
					} else {
						j++;
					}
				}
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (i != t.output.length) {
					fail(String.format("%s: unexpected number of row %d; want %d", t.name, i, t.output.length));
				}
			} catch (ScanException e) {
				if (t.error != null) {
					if (!e.getMessage().contains(t.error)) {
						fail(String.format("%s: error '%s', want error '%s'", t.name, e, t.error));
					} else if (t.line != 0 && (t.line != r.lineno() || t.column != j + 1)) {
						fail(String.format("%s: error at %d:%d expected %d:%d", t.name, r.lineno(), j + 1, t.line, t.column));
					}
				} else {
					fail(String.format("%s: unexpected error '%s'", t.name, e));
				}
			} finally {
				r.close();
			}
		}
	}

	@Test
	public void testScanRow() throws IOException {
		CsvScanner r;
		for (ReadTest t : ReadTest.tests) {
			r = new CsvScanner(new StringReader(t.input), t.sep, t.quoted);
			r.setCommentMarker(t.comment);
			r.setTrim(t.trim);
			r.setSkipEmptyLines(t.skipEmptyLines);

			int i = 0, j;
			String[] values = new String[10];
			try {
				while ((j = r.scanRow(values)) > 0) {
					if (i >= t.output.length) {
						fail(String.format("%s: unexpected number of row %d; want %d max", t.name, i + 1, t.output.length));
					} else if (j != t.output[i].length) {
						fail(String.format("%s: unexpected number of column %d; want %d at line %d", t.name, j, t.output[i].length, i + 1));
					}
					String[] row = Arrays.copyOf(values, j);
					if (!Arrays.equals(row, t.output[i])) {
						fail(String.format("%s: unexpected row %s; want %s at line %d", t.name,
								Arrays.toString(row), Arrays.toString(t.output[i]), i + 1));
					}
					i++;
				}
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (i != t.output.length) {
					fail(String.format("%s: unexpected number of row %d; want %d", t.name, i, t.output.length));
				}
			} catch (ScanException e) {
				if (t.error != null) {
					if (!e.getMessage().contains(t.error)) {
						fail(String.format("%s: error '%s', want error '%s'", t.name, e, t.error));
					} else if (t.line != 0 && t.line != r.lineno()) {
						fail(String.format("%s: error at %d expected %d:%d", t.name, r.lineno(), t.line, t.column));
					}
				} else {
					fail(String.format("%s: unexpected error '%s'", t.name, e));
				}
			} finally {
				r.close();
			}
		}
	}

	@Test
	public void testIterator() throws IOException {
		CsvScanner r;
		for (ReadTest t : ReadTest.tests) {
			r = new CsvScanner(new StringReader(t.input), t.sep, t.quoted);
			r.setCommentMarker(t.comment);
			r.setTrim(t.trim);
			r.setSkipEmptyLines(t.skipEmptyLines);

			int i = 0, j = 0;
			try {
				while (!r.atEndOfFile()) {
					for (String field : r) {
						if (i >= t.output.length) {
							fail(String.format("%s: unexpected number of row %d; want %d max", t.name, i + 1, t.output.length));
						} else if (j >= t.output[i].length) {
							fail(String.format("%s: unexpected number of column %d; want %d at line %d", t.name, j + 1, t.output[i].length, i + 1));
						}
						if (!t.output[i][j].equals(field)) {
							fail(String.format("%s: unexpected value '%s'; want '%s' at line %d, column %d", t.name, r.token(), t.output[i][j], i + 1, j + 1));
						}
						j++;
					}
					if (j != 0) {
						j = 0;
						i++;
					}
				}
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (i != t.output.length) {
					fail(String.format("%s: unexpected number of row %d; want %d", t.name, i, t.output.length));
				}
			} catch (Exception e) {
				if (t.error != null) {
					if (!e.getMessage().contains(t.error)) {
						fail(String.format("%s: error '%s', want error '%s'", t.name, e, t.error));
					} else if (t.line != 0 && (t.line != r.lineno() || t.column != j + 1)) {
						fail(String.format("%s: error at %d:%d expected %d:%d", t.name, r.lineno(), j + 1, t.line, t.column));
					}
				} else {
					fail(String.format("%s: unexpected error '%s'", t.name, e));
				}
			} finally {
				r.close();
			}
		}
	}

	@Test
	public void testScanObject() throws IOException {
		Object[] values = {"text", Math.PI, Math.PI, Integer.MAX_VALUE, Long.MAX_VALUE, true, 'c'};
		StringBuilder buffer = new StringBuilder();
		for (Object value : values) {
			buffer.append(String.valueOf(value));
			buffer.append(',');
		}
		CsvScanner r = new CsvScanner(new StringReader(buffer.toString()));
		assertEquals(values[0], r.scanText());
		assertEquals(((Number) values[1]).floatValue(), r.scanFloat(), 1e-6);
		assertEquals(((Number) values[2]).doubleValue(), r.scanDouble(), 1e-9);
		assertEquals(((Number) values[3]).intValue(), r.scanInt());
		assertEquals(((Number) values[4]).longValue(), r.scanLong());
		assertEquals(values[5], r.scanBool("true"));
		assertEquals(values[6], r.scanChar());
		assertEquals("", r.scanText()); // extra ',' during join...
		assertNull(r.scanText()); // eof
		r.close();
	}

	@Test
	public void testNullReader() {
		try {
			new CsvScanner(null);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("null"));
		}
	}

	@Test
	public void testLargeBuffer() throws IOException {
		char[] chars = new char[4096 + 1024];
		Arrays.fill(chars, 'c');
		chars[4200] = ',';
		final CsvScanner r = new CsvScanner(new CharArrayReader(chars));
		assertTrue(r.scan());
		assertEquals(4200, r.value().length());
		assertTrue(r.scan());
		assertEquals(919, r.value().length());
		assertFalse(r.scan());
		r.close();
	}

	@Test
	public void testPeek() throws IOException {
		CsvScanner r = new CsvScanner(new StringReader("a,b,c,d,e"));
		char[] peeks = new char[]{'b', 'c', 'd', 'e', '\0'};
		int i = 0;
		while (r.scan()) {
			final char c = r.peek();
			assertEquals(peeks[i], c);
			i++;
		}
		r.close();
	}
	// TODO scanRow with values = 0, 1, ...
}
