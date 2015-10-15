package org.bufio;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

class WriteTest {
	final String name;
	final String[][] input;
	final String output;
	char sep = ',';
	boolean quoted;
	boolean useCRLF;
	char comment;
	String error;

	CsvWriter createWriter(StringWriter s) {
		CsvWriter w = new CsvWriter(s, sep, quoted);
		w.setCommentMarker(comment);
		if (useCRLF) {
			w.useCRLF();
		}
		return w;
	}
	CsvColWriter createColWriter(StringWriter s) {
		CsvColWriter w = new CsvColWriter(s, sep, quoted);
		w.setCommentMarker(comment);
		if (useCRLF) {
			w.useCRLF();
		}
		return w;
	}

	private WriteTest(String name, String[][] input, String output) {
		this.name = name;
		this.input = input;
		this.output = output;
	}

	private WriteTest(String name, boolean quoted, String[][] input, String output) {
		this(name, input, output);
		this.quoted = quoted;
	}

  /*private WriteTest withSeparator(char sep) {
		this.sep = sep;
		return this;
	}*/

	private WriteTest useCRLF() {
		useCRLF = true;
		return this;
	}

	/*private WriteTest withComment(char comment) {
		this.comment = comment;
		return this;
	}*/

	private WriteTest withError(String error) {
		this.error = error;
		return this;
	}

	static final List<WriteTest> tests = new ArrayList<WriteTest>();

	static {
		tests.add(new WriteTest("Single", new String[][]{{"abc"}}, "abc\n"));
		tests.add(new WriteTest("SingleCRLF", new String[][]{{"abc"}}, "abc\r\n").useCRLF());
		tests.add(new WriteTest("DoubleQuoted", new String[][]{{"\"abc\""}}, "\"abc\"\n"));
		tests.add(new WriteTest("EscapedQuotes", true, new String[][]{{"\"abc\""}}, "\"\"\"abc\"\"\"\n"));
		tests.add(new WriteTest("DoubleQuote", new String[][]{{"a\"b"}}, "a\"b\n"));
		tests.add(new WriteTest("EscapedQuote", true, new String[][]{{"a\"b"}}, "\"a\"\"b\"\n"));
		tests.add(new WriteTest("NoEscape", new String[][]{{"\"a\"b\""}}, "\"a\"b\"\n"));
		tests.add(new WriteTest("EscapeInQuotedMode", true, new String[][]{{"\"a\"b\""}}, "\"\"\"a\"\"b\"\"\"\n"));
		tests.add(new WriteTest("Space", new String[][]{{" abc"}}, " abc\n"));
		tests.add(new WriteTest("SpaceInQuotedMode", true, new String[][]{{" abc"}}, " abc\n"));
		tests.add(new WriteTest("Sep", new String[][]{{"abc,def"}}, null).withError("separator in value"));
		tests.add(new WriteTest("SepInQuotedMode", true, new String[][]{{"abc,def"}}, "\"abc,def\"\n"));
		tests.add(new WriteTest("Simple", new String[][]{{"abc", "def"}}, "abc,def\n"));
		tests.add(new WriteTest("SimpleInQuotedMode", true, new String[][]{{"abc", "def"}}, "abc,def\n"));
		tests.add(new WriteTest("TwoRecords", new String[][]{{"abc"}, {"def"}}, "abc\ndef\n"));
		tests.add(new WriteTest("TwoRecordsInQuotedMode", true, new String[][]{{"abc"}, {"def"}}, "abc\ndef\n"));
		tests.add(new WriteTest("NewLine", new String[][]{{"abc\ndef"}}, null).withError("newline character in value"));
		tests.add(new WriteTest("NewLineInQuotedMode", true, new String[][]{{"abc\ndef"}}, "\"abc\ndef\"\n"));
		tests.add(new WriteTest("NewLineInQuotedModeCRLF", true, new String[][]{{"abc\ndef"}}, "\"abc\ndef\"\r\n").useCRLF());
		tests.add(new WriteTest("CRInQuotedMode", true, new String[][]{{"abc\rdef"}}, "\"abc\rdef\"\n"));
		tests.add(new WriteTest("CRInQuotedModeCRLF", true, new String[][]{{"abc\rdef"}}, "\"abc\rdef\"\r\n").useCRLF());
		tests.add(new WriteTest("Empty", true, new String[][]{{""}}, "\n"));
		tests.add(new WriteTest("Empty2", true, new String[][]{{"", ""}}, ",\n"));
		tests.add(new WriteTest("Empty3", true, new String[][]{{"", "", ""}}, ",,\n"));
		tests.add(new WriteTest("A3", true, new String[][]{{"", "", "a"}}, ",,a\n"));
		tests.add(new WriteTest("A2", true, new String[][]{{"", "a", ""}}, ",a,\n"));
		tests.add(new WriteTest("A23", true, new String[][]{{"", "a", "a"}}, ",a,a\n"));
		tests.add(new WriteTest("A1", true, new String[][]{{"a", "", ""}}, "a,,\n"));
		tests.add(new WriteTest("A13", true, new String[][]{{"a", "", "a"}}, "a,,a\n"));
		tests.add(new WriteTest("A12", true, new String[][]{{"a", "a", ""}}, "a,a,\n"));
		tests.add(new WriteTest("A123", true, new String[][]{{"a", "a", "a"}}, "a,a,a\n"));
		tests.add(new WriteTest("Escape", true, new String[][]{{"\\."}}, "\\.\n"));
	}
}
