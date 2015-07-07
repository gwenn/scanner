package org.bufio;

/** Sets the component used by {@link CsvReader#getObject} to transform text to object. */
public interface Unmarshaler {
	<T> T unmarshal(String text, Class<T> type) throws ScanException;
}
