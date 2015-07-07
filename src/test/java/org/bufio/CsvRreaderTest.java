package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CsvRreaderTest {
	@Test
	public void test() throws IOException {
		CsvReader r;
		for (ReadTest t : ReadTest.tests) {
			r = t.createReader();

			check(t, r, false);

			r.reset(new StringReader(t.input));
			check(t, r, true);
		}
	}

	static void check(ReadTest t, CsvReader r, boolean close) throws IOException {
		int i = 0;
		try {
			while (r.next()) {
				String[] row = r.values();
				if (i >= t.output.length) {
					fail(String.format("%s: unexpected number of row %d; want %d max", t.name, i + 1, t.output.length));
				} else if (row.length != t.output[i].length) {
					fail(String.format("%s: unexpected number of column %d; want %d at line %d", t.name, row.length, t.output[i].length, i + 1));
				}
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
				} else if (t.line != 0 && t.line != r.getRow()) {
					fail(String.format("%s: error at %d expected %d:%d", t.name, r.getRow(), t.line, t.column));
				}
			} else {
				fail(String.format("%s: unexpected error '%s'", t.name, e));
			}
		} finally {
			if (close) {
				r.close();
			}
		}
	}

	@Test
	public void testGetByIndex() throws IOException {
		Object[] values = {"text", Math.PI, Math.PI, Integer.MAX_VALUE, Long.MAX_VALUE, true, Byte.MAX_VALUE, Short.MAX_VALUE};
		CsvReader r = new CsvReader(new StringReader(join(values)));
		assertTrue(r.next());
		assertEquals(8, r.getColumnCount());
		assertEquals(values[0], r.getString(1));
		assertEquals(((Number) values[1]).floatValue(), r.getFloat(2), 1e-6);
		assertEquals(((Number) values[2]).doubleValue(), r.getDouble(3), 1e-9);
		assertEquals(((Number) values[3]).intValue(), r.getInt(4));
		assertEquals(((Number) values[4]).longValue(), r.getLong(5));
		//assertEquals(values[5], r.get(6));
		assertEquals(values[6], r.getByte(7));
		assertEquals(values[7], r.getShort(8));
		r.close();
	}

	@Test
	public void testGetByName() throws IOException {
		Object[] values = {"text", Math.PI, Math.PI, Integer.MAX_VALUE, Long.MAX_VALUE, true, Byte.MAX_VALUE, Short.MAX_VALUE};
		CsvReader r = new CsvReader(new StringReader(join(values)));
		assertTrue(r.next());
		assertEquals(8, r.getColumnCount());
		String[] headers = new String[]{"", "string", "float", "double", "int", "long", "bool", "byte", "short"};
		r.withColumnIndexes(toMap(headers));
		for (int i = 1; i < headers.length; i++) {
			assertEquals(i, r.findColumn(headers[i]));
			assertEquals(headers[i], r.getColumnLabel(i));
		}
		assertEquals(values[0], r.getString(headers[1]));
		assertEquals(((Number) values[1]).floatValue(), r.getFloat(headers[2]), 1e-6);
		assertEquals(((Number) values[2]).doubleValue(), r.getDouble(headers[3]), 1e-9);
		assertEquals(((Number) values[3]).intValue(), r.getInt(headers[4]));
		assertEquals(((Number) values[4]).longValue(), r.getLong(headers[5]));
		//assertEquals(values[5], r.get(headers[6]));
		assertEquals(values[6], r.getByte(headers[7]));
		assertEquals(values[7], r.getShort(headers[8]));
		r.close();
	}

	@Test
	public void testScanHeaders() throws IOException {
		String[] headers = new String[]{"string", "float", "double", "int", "long", "bool", "byte", "short"};
		CsvReader r = new CsvReader(new StringReader(join(headers)));
		r.scanHeaders(false);
		assertEquals(8, r.getColumnCount());
		for (int i = 0; i < headers.length; i++) {
			assertEquals(i + 1, r.findColumn(headers[i]));
			assertEquals(headers[i], r.getColumnLabel(i + 1));
		}
		r.close();
	}

	private static Map<String, Integer> toMap(String[] headers) {
		Map<String, Integer> columnIndexes = new HashMap<String, Integer>(headers.length);
		for (int i = 1; i < headers.length; i++) {
			columnIndexes.put(headers[i], i);
		}
		return columnIndexes;
	}

	@Test
	public void testEmptyIsNull() throws IOException {
		Object[] values = {"", "", "", "", "", "", "", ""};
		CsvReader r = new CsvReader(new StringReader(join(values)));
		r.setEmptyIsNull(true);
		assertTrue(r.next());
		assertEquals(8, r.getColumnCount());
		assertNull(r.getString(1));
		assertTrue(r.wasNull());
		assertEquals(0f, r.getFloat(2), 1e-6);
		assertTrue(r.wasNull());
		assertEquals(0d, r.getDouble(3), 1e-9);
		assertTrue(r.wasNull());
		assertEquals(0, r.getInt(4));
		assertTrue(r.wasNull());
		assertEquals(0l, r.getLong(5));
		assertTrue(r.wasNull());
		assertEquals(0, r.getByte(7));
		assertTrue(r.wasNull());
		assertEquals(0, r.getShort(8));
		assertTrue(r.wasNull());
		r.close();
	}

	@Test
	public void testGetWithoutNext() throws IOException {
		CsvReader r = new CsvReader(new StringReader(""));
		try {
			r.getString(1);
			fail();
		} catch (ScanException e) {
			assertEquals("No row", e.getMessage());
		}
	}

	@Test
	public void testGetWithBadIndex() throws IOException {
		CsvReader r = new CsvReader(new StringReader(" "));
		assertTrue(r.next());
		assertEquals(1, r.getColumnCount());
		try {
			r.getString(0);
			fail();
		} catch (ScanException e) {
			assertEquals("Index out of bound (0 < 1)", e.getMessage());
		}
		try {
			r.getString(2);
			fail();
		} catch (ScanException e) {
			assertEquals("Index out of bound (2 > 1)", e.getMessage());
		}
	}

	@Test
	public void testBadColumnIndexes() throws IOException {
		CsvReader r = new CsvReader(new StringReader(""));
		try {
			r.withColumnIndexes(Collections.<String, Integer>singletonMap("null", null));
			fail();
		} catch (Exception e) {
			assertEquals("Null index for 'null'", e.getMessage());
		}
		try {
			r.withColumnIndexes(Collections.singletonMap("zero", 0));
			fail();
		} catch (Exception e) {
			assertEquals("Invalid index for 'zero': 0 < 1", e.getMessage());
		}
	}

	@Test
	public void testNoHeader() throws IOException {
		CsvReader r = new CsvReader(new StringReader(""));
		try {
			r.findColumn("header");
			fail();
		} catch (IOException e) {
			assertEquals("No header", e.getMessage());
		}
		try {
			r.getColumnLabel(1);
			fail();
		} catch (IOException e) {
			assertEquals("No header", e.getMessage());
		}
		r.withColumnIndexes(Collections.singletonMap("header", 1));
		try {
			r.findColumn("unknown");
			fail();
		} catch (IOException e) {
			assertEquals("No such column 'unknown' in [header]", e.getMessage());
		}
		try {
			r.getColumnLabel(0);
			fail();
		} catch (IOException e) {
			assertEquals("Index out of bound (0 < 1)", e.getMessage());
		}
		try {
			r.getColumnLabel(2);
			fail();
		} catch (IOException e) {
			assertEquals("No such index 2 in {header=1}", e.getMessage());
		}
	}

	@Test
	public void testSkipRow() throws IOException {
		CsvReader r = new CsvReader(new StringReader(
				"colA,colB\n# comment...\nvalue11,value12\n# comment...\nvalue21,value22"));
		r.setCommentMarker('#');
		r.skipRows(1);
		assertTrue(r.next());
		assertEquals(2, r.getColumnCount());
		assertArrayEquals(new String[]{"value11", "value12"}, r.values());
		assertTrue(r.next());
		assertEquals(2, r.getColumnCount());
		assertArrayEquals(new String[]{"value21", "value22"}, r.values());
		r.close();
	}

	@Test
	public void testSkipRows() throws IOException {
		CsvReader r = new CsvReader(new StringReader(
				"colA,colB\n# comment...\nvalue11,value12\n# comment...\nvalue21,value22"));
		r.skipRows(2);
		assertTrue(r.next());
		assertEquals(2, r.getColumnCount());
		assertArrayEquals(new String[]{"value11", "value12"}, r.values());
		r.skipRows(1);
		assertTrue(r.next());
		assertEquals(2, r.getColumnCount());
		assertArrayEquals(new String[]{"value21", "value22"}, r.values());
		r.close();
	}

	private static String join(Object[] values) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				buffer.append(',');
			}
			buffer.append(String.valueOf(values[i]));
		}
		return buffer.toString();
	}
}
