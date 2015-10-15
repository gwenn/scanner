package org.bufio;

import java.io.IOException;

/** Component called by {@link CsvWriter#writeValue} to marshall value to text. */
public interface Marshaler {
	String marshal(Object o) throws IOException;
}
