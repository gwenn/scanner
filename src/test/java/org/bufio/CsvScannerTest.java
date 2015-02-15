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
  private static class TestCase {
    private String name;
    private String input;
    private String[][] output;

    private char sep;
    private boolean quoted;
    private boolean trim;
    private char comment;

    private String error;
    private int line; // expected error line if != 0
    private int column;

    private TestCase(String name, String input, String[][] output) {
      this.name = name;
      this.input = input;
      this.output = output;
    }

    public TestCase(String name, boolean quoted, String input, String[][] output) {
      this(name, input, output);
      this.quoted = quoted;
    }
    public TestCase(String name, char sep, String input, String[][] output) {
      this(name, input, output);
      this.sep = sep;
    }
    public TestCase(String name, boolean quoted, boolean trim, String input, String[][] output) {
      this(name, input, output);
      this.quoted = quoted;
      this.trim = trim;
    }
    public TestCase(String name, boolean quoted, char comment, String input, String[][] output) {
      this(name, input, output);
      this.quoted = quoted;
      this.comment = comment;
    }
    public TestCase(String name, boolean quoted, String input, String[][] output, String error, int line, int column) {
      this(name, input, output);
      this.quoted = quoted;
      this.error = error;
      this.line = line;
      this.column = column;
    }
  }

  private List<TestCase> tests = new ArrayList<TestCase>();

  {
    tests.add(new TestCase("Simple", "a,b,c\n", new String[][]{{"a", "b", "c"}}));
    tests.add(new TestCase("CRLF", "a,b\r\nc,d\r\n", new String[][]{{"a", "b"}, {"c", "d"}}));
    tests.add(new TestCase("CRLFQuoted", true, "a,b\r\nc,\"d\"\r\n", new String[][]{{"a", "b"}, {"c", "d"}}));
    tests.add(new TestCase("BareCR", "a,b\rc,d\r\n", new String[][]{{"a", "b\rc", "d"}}));
    tests.add(new TestCase("RFC4180test", true, "#field1,field2,field3\n" +
        "\"aaa\",\"bb\n" +
        "b\",\"ccc\"\n" +
        "\"a,a\",\"b\"\"bb\",\"ccc\"\n" +
        "zzz,yyy,xxx", new String[][]{{"#field1", "field2", "field3"},
        {"aaa", "bb\nb", "ccc"},
        {"a,a", "b\"bb", "ccc"},
        {"zzz", "yyy", "xxx"},}));
    tests.add(new TestCase("NoEOLTest", "a,b,c", new String[][]{{"a", "b", "c"}}));
    tests.add(new TestCase("Semicolon", ';', "a;b;c\n", new String[][]{{"a", "b", "c"}}));
    tests.add(new TestCase("MultiLine", true, "\"two\n" +
        "line\",\"one line\",\"three\n" +
        "line\n" +
        "field\"", new String[][]{{"two\nline", "one line", "three\nline\nfield"}}));
    tests.add(new TestCase("EmbeddedNewline", true, "a,\"b\n" +
        "b\",\"c\n" +
        "\n" +
        "\",d", new String[][]{{"a", "b\nb", "c\n\n", "d"}}));
    tests.add(new TestCase("EscapedQuoteAndEmbeddedNewLine", true, "\"a\"\"b\",\"c\"\"\r\nd\"",
        new String[][]{{"a\"b", "c\"\r\nd"}}));
    tests.add(new TestCase("BlankLine", true, "a,b,\"c\"\n\nd,e,f\n\n",
        new String[][]{{"a", "b", "c"}, {"d", "e", "f"}}));
    tests.add(new TestCase("TrimSpace", false, true, " a,  b,   c\n", new String[][]{{"a", "b", "c"}}));
    tests.add(new TestCase("TrimSpaceQuoted", true, true, " a,b ,\" c \", d \n", new String[][]{{"a", "b", " c ", "d"}}));
    tests.add(new TestCase("LeadingSpace", " a,  b,   c\n", new String[][]{{" a", "  b", "   c"}}));
    tests.add(new TestCase("Comment", false, '#', "#1,2,3\na,b,#\n#comment\nc\n# comment",
        new String[][]{{"a", "b", "#"}, {"c"}}));
    tests.add(new TestCase("NoComment", "#1,2,3\na,b,c", new String[][]{{"#1", "2", "3"}, {"a", "b", "c"}}));
    tests.add(new TestCase("StrictQuotes", true, "a \"word\",\"1\"2\",a\",\"b",
        new String[][]{{"a \"word\"", "1\"2", "a\"", "b"}},
        "unescaped \" character", 1, 2));
    // TODO LazyQuotes, BareQuotes,
    tests.add(new TestCase("BareDoubleQuotes", true, "a\"\"b,c", new String[][]{{"a\"\"b", "c"}}));
    tests.add(new TestCase("TrimQuote", true, true, " \"a\",\" b\",c", new String[][]{{"\"a\"", " b", "c"}}));
    tests.add(new TestCase("BareQuote", true, "a \"word\",\"b\"", new String[][]{{"a \"word\"", "b"}}));
    tests.add(new TestCase("TrailingQuote", true, "\"a word\",b\"", new String[][]{{"a word", "b\""}}));
    tests.add(new TestCase("ExtraneousQuote", true, "\"a \"word\",\"b\"", null, "unescaped \" character", 1, 1));
    tests.add(new TestCase("FieldCount", "a,b,c\nd,e", new String[][]{{"a", "b", "c"}, {"d", "e"}}));
    tests.add(new TestCase("TrailingCommaEOF", "a,b,c,", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaEOL", "a,b,c,\n", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaSpaceEOF", false, true, "a,b,c, ", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaSpaceEOL", false, true, "a,b,c, \n", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaLine3", "a,b,c\nd,e,f\ng,hi,",
        new String[][]{{"a", "b", "c"}, {"d", "e", "f"}, {"g", "hi", ""}}));
    tests.add(new TestCase("NotTrailingComma3", "a,b,c, \n", new String[][]{{"a", "b", "c", " "}}));
    tests.add(new TestCase("CommaFieldTest", true, "x,y,z,w\n" +
        "x,y,z,\n" +
        "x,y,,\n" +
        "x,,,\n" +
        ",,,\n" +
        "\"x\",\"y\",\"z\",\"w\"\n" +
        "\"x\",\"y\",\"z\",\"\"\n" +
        "\"x\",\"y\",\"\",\"\"\n" +
        "\"x\",\"\",\"\",\"\"\n" +
        "\"\",\"\",\"\",\"\"\n", new String[][]{{"x", "y", "z", "w"},
        {"x", "y", "z", ""},
        {"x", "y", "", ""},
        {"x", "", "", ""},
        {"", "", "", ""},
        {"x", "y", "z", "w"},
        {"x", "y", "z", ""},
        {"x", "y", "", ""},
        {"x", "", "", ""},
        {"", "", "", ""}}));
    tests.add(new TestCase("TrailingCommaIneffective1", "a,b,\nc,d,e", new String[][]{{"a", "b", ""},
        {"c", "d", "e"}}));
    tests.add(new TestCase("EmptyFields", "a,,\n,,e", new String[][]{{"a", "", ""},
        {"", "", "e"}}));
    tests.add(new TestCase("6287", "Field1,Field2,\"LazyQuotes\" Field3,Field4,Field5",
        new String[][]{{"Field1", "Field2", "\"LazyQuotes\" Field3", "Field4", "Field5"}}));
    tests.add(new TestCase("6258", true, "\"Field1\",\"Field2 \"LazyQuotes\"\",\"Field3\",\"Field4\"",
        new String[][]{{"Field1", "Field2 \"LazyQuotes\"", "Field3", "Field4"}},
        "unescaped \" character", 1, 2));
    tests.add(new TestCase("3150", '\t', "3376027\t”S” Falls\t\"S\" Falls\t\t4.53333",
        new String[][]{{"3376027", "”S” Falls", "\"S\" Falls", "", "4.53333"}}));
  }

  @Test
  public void testScan() {
    CsvScanner r;
    for (TestCase t : tests) {
      r = new CsvScanner(new StringReader(t.input), t.sep == 0 ? ',' : t.sep, t.quoted);
      r.setCommentMarker(t.comment);
      r.setTrim(t.trim);

      int i = 0, j = 0;
      try {
        while (r.scan()) {
          if (j == 0 && r.atEndOfRow() && r.token().isEmpty()) { // skip empty lines
            continue;
          }
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
      } catch (IOException e) {
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
  public void testScanRow() {
    CsvScanner r;
    for (TestCase t : tests) {
      r = new CsvScanner(new StringReader(t.input), t.sep == 0 ? ',' : t.sep, t.quoted);
      r.setCommentMarker(t.comment);
      r.setTrim(t.trim);

      int i = 0, j = 0;
      String[] values = new String[10];
      try {
        while ((j = r.scanRow(values)) > 0) {
          if (i >= t.output.length) {
            fail(String.format("%s: unexpected number of row %d; want %d max", t.name, i + 1, t.output.length));
          } else if (j != t.output[i].length) {
            fail(String.format("%s: unexpected number of column %d; want %d at line %d", t.name, j + 1, t.output[i].length, i + 1));
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
      } catch (IOException e) {
        if (t.error != null) {
          if (!e.getMessage().contains(t.error)) {
            fail(String.format("%s: error '%s', want error '%s'", t.name, e, t.error));
          } else if (t.line != 0 && t.line != r.lineno()) {
            fail(String.format("%s: error at %d expected %d:%d", t.name, r.lineno(), t.line, t.column));
          }
        } else {
          fail(String.format("%s: unexpected error '%s'", t.name, e));
        }
      }
    }
  }

  // TODO scanRow with values = 0, 1, ...
  // TODO skipRow "colA,colB\n# comment...\nvalue11,value12\n# comment...\nvalue21,value22"
}