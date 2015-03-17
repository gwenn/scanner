package org.bufio;

import java.util.ArrayList;
import java.util.List;

class TestCase {
  final String name;
  final String input;
  final String[][] output;

  char sep = ',';
  boolean quoted;
  boolean trim;
  char comment;
  boolean skipEmptyLines = true;

  String error;
  int line; // expected error line if != 0
  int column;

  TestCase(String name, String input, String[][] output) {
    this.name = name;
    this.input = input;
    this.output = output;
  }

  TestCase(String name, boolean quoted, String input, String[][] output) {
    this(name, input, output);
    this.quoted = quoted;
  }

  private TestCase withSeparator(char sep) {
    this.sep = sep;
    return this;
  }

  private TestCase withTrim() {
    trim = true;
    return this;
  }

  private TestCase withComment(char comment) {
    this.comment = comment;
    return this;
  }

  private TestCase withError(String error, int line, int column) {
    this.error = error;
    this.line = line;
    this.column = column;
    return this;
  }
  private TestCase withEmptyLines() {
    this.skipEmptyLines = false;
    return this;
  }

  static List<TestCase> tests = new ArrayList<TestCase>();

  static {
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
    tests.add(new TestCase("Semicolon", "a;b;c\n", new String[][]{{"a", "b", "c"}}).withSeparator(';'));
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
    tests.add(new TestCase("EmptyLine", true, "a,b,\"c\"\n\nd,e,f\n\n",
        new String[][]{{"a", "b", "c"}, {""}, {"d", "e", "f"}, {""}}).withEmptyLines());
    tests.add(new TestCase("TrimSpace", false, " a,  b,   c\n", new String[][]{{"a", "b", "c"}}).withTrim());
    tests.add(new TestCase("TrimSpaceQuoted", true, " a,b ,\" c \", d \n", new String[][]{{"a", "b", " c ", "d"}}).withTrim());
    tests.add(new TestCase("LeadingSpace", " a,  b,   c\n", new String[][]{{" a", "  b", "   c"}}));
    tests.add(new TestCase("Comment", false, "#1,2,3\na,b,#\n#comment\nc\n# comment",
        new String[][]{{"a", "b", "#"}, {"c"}}).withComment('#'));
    tests.add(new TestCase("NoComment", "#1,2,3\na,b,c", new String[][]{{"#1", "2", "3"}, {"a", "b", "c"}}));
    tests.add(new TestCase("StrictQuotes", true, "a \"word\",\"1\"2\",a\",\"b",
        new String[][]{{"a \"word\"", "1\"2", "a\"", "b"}}).withError("unescaped \" character", 1, 2));
    // TODO LazyQuotes, BareQuotes,
    tests.add(new TestCase("BareDoubleQuotes", true, "a\"\"b,c", new String[][]{{"a\"\"b", "c"}}));
    tests.add(new TestCase("TrimQuote", true, " \"a\",\" b\",c", new String[][]{{"\"a\"", " b", "c"}}).withTrim());
    tests.add(new TestCase("BareQuote", true, "a \"word\",\"b\"", new String[][]{{"a \"word\"", "b"}}));
    tests.add(new TestCase("TrailingQuote", true, "\"a word\",b\"", new String[][]{{"a word", "b\""}}));
    tests.add(new TestCase("ExtraneousQuote", true, "\"a \"word\",\"b\"", null).
        withError("unescaped \" character", 1, 1));
    tests.add(new TestCase("FieldCount", "a,b,c\nd,e", new String[][]{{"a", "b", "c"}, {"d", "e"}}));
    tests.add(new TestCase("TrailingCommaEOF", "a,b,c,", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaEOL", "a,b,c,\n", new String[][]{{"a", "b", "c", ""}}));
    tests.add(new TestCase("TrailingCommaSpaceEOF", false, "a,b,c, ", new String[][]{{"a", "b", "c", ""}}).withTrim());
    tests.add(new TestCase("TrailingCommaSpaceEOL", false, "a,b,c, \n", new String[][]{{"a", "b", "c", ""}}).withTrim());
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
        new String[][]{{"Field1", "Field2 \"LazyQuotes\"", "Field3", "Field4"}}).
        withError("unescaped \" character", 1, 2));
    tests.add(new TestCase("3150", "3376027\t”S” Falls\t\"S\" Falls\t\t4.53333",
        new String[][]{{"3376027", "”S” Falls", "\"S\" Falls", "", "4.53333"}}).withSeparator('\t'));
  }
}
