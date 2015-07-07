package org.bufio;

/** Component called by {@link CsvWriter#writeValue} to marshall value to text. */
public interface Marshaler {
	String marshal(Object o) throws ScanException;
}
