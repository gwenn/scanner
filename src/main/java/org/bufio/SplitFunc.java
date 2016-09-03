package org.bufio;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Split function used to tokenize the input
 */
@FunctionalInterface
public interface SplitFunc<T> {
	T split(@Nonnull char[] data, @Nonnegative int start, @Nonnegative int end, boolean atEOF) throws ScanException;
}
