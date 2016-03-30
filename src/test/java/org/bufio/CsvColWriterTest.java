package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class CsvColWriterTest {
	@Test
	public void testWriteRow() throws IOException {
		for (WriteTest t : WriteTest.tests) {
			final StringWriter s = new StringWriter();
			try (CsvColWriter w = t.createColWriter(s)) {
				for (String[] row : t.input) {
					final List<Integer> indexes = randomIndexes(row.length);
					for (int i : indexes) {
						w.setString(i, row[i - 1]);
					}
					w.endOfRow();
				}
				w.flush();
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (!t.output.equals(s.toString())) {
					fail(String.format("%s:%n%s%n<>%n%s%n", t.name, t.output, s.toString()));
				}
			} catch (IOException e) {
				if (t.error != null) {
					if (!e.getMessage().contains(t.error)) {
						fail(String.format("%s: error '%s', want error '%s'", t.name, e, t.error));
					}
				} else {
					fail(String.format("%s: unexpected error '%s'", t.name, e));
				}
			}
		}
	}

	@Test
	public void testSetByName() throws IOException {
		Object[] values = {"text", (float)Math.PI, Math.PI, Integer.MAX_VALUE, Long.MAX_VALUE, true, Byte.MAX_VALUE, Short.MAX_VALUE};
		final StringWriter buff = new StringWriter();
		CsvColWriter w = new CsvColWriter(buff);
		String[] headers = new String[]{"string", "float", "double", "int", "long", "bool", "byte", "short"};
		w.withHeaders(Arrays.asList(headers));
		for (int i = 1; i <= headers.length; i++) {
			assertEquals(i, w.findColumn(headers[i-1]));
			assertEquals(headers[i-1], w.getColumnLabel(i));
		}
		w.writerHeaders(false);
		w.setString(headers[0], (String)values[0]);
		w.setFloat(headers[1], (Float) values[1]);
		w.setDouble(headers[2], (Double) values[2]);
		w.setInt(headers[3], (Integer) values[3]);
		w.setLong(headers[4], (Long) values[4]);
		w.setObject(headers[5], values[5]);
		w.setByte(headers[6], (Byte) values[6]);
		w.setShort(headers[7], (Short) values[7]);
		w.endOfRow();
		w.close();

		CsvReader r = new CsvReader(new StringReader(buff.toString()));
		r.scanHeaders(false);
		for (int i = 1; i <= headers.length; i++) {
			assertEquals(i, r.findColumn(headers[i-1]));
			assertEquals(headers[i-1], r.getColumnLabel(i));
		}
		assertTrue(r.next());
		assertEquals(8, r.getColumnCount());
		assertEquals(values[0], r.getString(headers[0]));
		assertEquals(((Number) values[1]).floatValue(), r.getFloat(headers[1]), 1e-6);
		assertEquals(((Number) values[2]).doubleValue(), r.getDouble(headers[2]), 1e-9);
		assertEquals(((Number) values[3]).intValue(), r.getInt(headers[3]));
		assertEquals(((Number) values[4]).longValue(), r.getLong(headers[4]));
		//assertEquals(values[5], r.get(headers[5]));
		assertEquals(values[6], r.getByte(headers[6]));
		assertEquals(values[7], r.getShort(headers[7]));
		r.close();
	}

	private static List<Integer> randomIndexes(int length) {
		final List<Integer> indexes = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			indexes.add(i + 1);
		}
		Collections.shuffle(indexes);
		return indexes;
	}
}
