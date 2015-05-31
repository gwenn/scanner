package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class CsvRreaderTest {
  @Test
  public void test() throws IOException {
    CsvReader r;
    for (ReadTest t : ReadTest.tests) {
      r = new CsvReader(new StringReader(t.input), t.sep, t.quoted);
      r.setCommentMarker(t.comment);
      r.setTrim(t.trim);
      r.setSkipEmptyLines(t.skipEmptyLines);

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
        r.close();
      }
    }
  }
}
