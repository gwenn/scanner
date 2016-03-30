package org.bufio;

/** Component used by {@link CsvReader#getObject} to transform text to object. */
@FunctionalInterface
public interface Unmarshaler {
	<T> T unmarshal(String text, Class<T> type) throws ScanException;
}
