package org.bufio;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.fail;

public class CsvScannerTest {
  @Test
  public void testScan() throws IOException {
    CsvScanner r;
    for (TestCase t : TestCase.tests) {
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
          if (!Objects.equals(r.token(), t.output[i][j])) {
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
    for (TestCase t : TestCase.tests) {
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

  // TODO scanRow with values = 0, 1, ...
  // TODO skipRow "colA,colB\n# comment...\nvalue11,value12\n# comment...\nvalue21,value22"
}