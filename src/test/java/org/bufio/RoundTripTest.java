package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RoundTripTest {
	@Test
	public void testReadWrite() throws IOException {
		for (ReadTest t : ReadTest.tests) {
			if (t.error != null) {
				continue;
			}
			CsvReader r = t.createReader();
			final StringWriter buf = new StringWriter();
			CsvWriter w = new CsvWriter(buf, t.sep, t.quoted);
			//w.setCommentMarker(t.comment);
			//w.useCRLF();
			while (r.next()) {
				w.writeRow(r.values());
			}
			r.close();
			w.close();
			if (!t.input.equals(buf.toString())) {
				r = new CsvReader(new StringReader(buf.toString()), t.sep, t.quoted);
				//r.setCommentMarker(t.comment); comments already skipped (or not)
				//r.setTrim(t.trim); values already trimmed (or not)
				r.setSkipEmptyLines(t.skipEmptyLines);
				CsvRreaderTest.check(t, r, true);
			}
		}
	}
	@Test
	public void testReadColWrite() throws IOException {
		for (ReadTest t : ReadTest.tests) {
			if (t.error != null) {
				continue;
			}
			CsvReader r = t.createReader();
			final StringWriter buf = new StringWriter();
			CsvColWriter w = new CsvColWriter(buf, t.sep, t.quoted);
			//w.setCommentMarker(t.comment);
			//w.useCRLF();
			while (r.next()) {
				final String[] values = r.values();
				for (int i = 1; i <= values.length; i++) {
					w.setString(i, values[i-1]);
				}
				w.endOfRow();
			}
			r.close();
			w.close();
			if (!t.input.equals(buf.toString())) {
				r = new CsvReader(new StringReader(buf.toString()), t.sep, t.quoted);
				//r.setCommentMarker(t.comment); comments already skipped (or not)
				//r.setTrim(t.trim); values already trimmed (or not)
				r.setSkipEmptyLines(t.skipEmptyLines);
				CsvRreaderTest.check(t, r, true);
			}
		}
	}

	@Test
	public void testWriteRead() throws IOException {
		for (WriteTest t : WriteTest.tests) {
			if (t.error != null) {
				continue;
			}
			final StringWriter buf = new StringWriter();
			CsvWriter w = t.createWriter(buf);
			for (String[] row : t.input) {
				w.writeRow(row);
			}
			CsvReader r = new CsvReader(new StringReader(buf.toString()), t.sep, t.quoted);
			r.setCommentMarker(t.comment);
			r.setSkipEmptyLines(false);
			int i = 0;
			while (r.next()) {
				assertArrayEquals(t.input[i], r.values());
				i++;
			}
			assertEquals(t.input.length, i);
			w.close();
		}
	}

	@Test
	public void testWriteResultSet() throws IOException, SQLException {
test:
	for (ReadTest t : ReadTest.tests) {
			if (t.error != null) {
				continue;
			}
			final int columnCount = t.output[0].length;
			for (String[] values : t.output) {
				if (values.length != columnCount) {
					continue test;
				}
			}
			CsvReader r = t.createReader();
			ResultSet rs = proxy(r, columnCount);
			final StringWriter buf = new StringWriter();
			CsvWriter w = new CsvWriter(buf, t.sep, t.quoted);
			//w.setCommentMarker(t.comment);
			//w.useCRLF();
			w.writeResultSet(rs, false);
			r.close();
			w.close();
			if (!t.input.equals(buf.toString())) {
				r = new CsvReader(new StringReader(buf.toString()), t.sep, t.quoted);
				//r.setCommentMarker(t.comment); comments already skipped (or not)
				//r.setTrim(t.trim); values already trimmed (or not)
				r.setSkipEmptyLines(t.skipEmptyLines);
				CsvRreaderTest.check(t, r, true);
			}
		}
	}

	private ResultSet proxy(final CsvReader r, final int columnCount) {
		return (ResultSet) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[]{ResultSet.class, ResultSetMetaData.class},
				(proxy, method, args) -> {
					final String methodName = method.getName();
					if ("getMetaData".equals(methodName)) {
						return proxy;
					} else if ("getColumnCount".equals(methodName)) {
						return columnCount;
					} else if ("next".equals(methodName)) {
						return r.next();
					} else if ("getObject".equals(methodName)) {
						return r.getString((Integer) args[0]);
					} else if ("toString".equals(methodName)) {
						return r.toString();
					}
					throw new UnsupportedOperationException(String.format("%s(%s)", methodName, Arrays.toString(args)));
				});
	}
}
