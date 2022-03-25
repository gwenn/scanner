package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.fail;

public class CsvWriterTest {
	@Test
	public void testWriteRow() throws IOException {
		for (WriteTest t : WriteTest.tests) {
			final StringWriter s = new StringWriter();
			try (CsvWriter w = t.createWriter(s)) {
				for (String[] row : t.input) {
					w.writeRow(row);
				}
				w.flush();
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (!t.output.equals(s.toString())) {
					fail(String.format("%s:%n%s%n<>%n%s%n", t.name, t.output, s));
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
	public void testWriteValue() throws IOException {
		for (WriteTest t : WriteTest.tests) {
			final StringWriter s = new StringWriter();
			try (CsvWriter w = t.createWriter(s)) {
				for (String[] row : t.input) {
					for (String v : row) {
						w.writeValue(v); // FIXME create specific tests...
					}
					w.endOfRow();
				}
				w.flush();
				if (t.error != null) {
					fail(String.format("%s: error '%s', want error '%s'", t.name, null, t.error));
				}
				if (!t.output.equals(s.toString())) {
					fail(String.format("%s:%n%s%n<>%n%s%n", t.name, t.output, s));
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
}
