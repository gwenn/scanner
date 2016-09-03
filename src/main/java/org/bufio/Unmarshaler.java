package org.bufio;

import javax.annotation.Nullable;

/** Component used by {@link CsvReader#getObject} to transform text to object. */
@FunctionalInterface
public interface Unmarshaler {
	@Nullable
	<T> T unmarshal(String text, Class<T> type) throws ScanException;
}
