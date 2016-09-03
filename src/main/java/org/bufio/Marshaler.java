package org.bufio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/** Component called by {@link CsvWriter#writeValue} to marshall value to text. */
@FunctionalInterface
public interface Marshaler {
	@Nonnull
	String marshal(@Nullable Object o) throws IOException;
}
