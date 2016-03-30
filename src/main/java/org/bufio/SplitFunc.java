package org.bufio;

/**
 * Split function used to tokenize the input
 */
@FunctionalInterface
public interface SplitFunc<T> {
	T split(char[] data, int start, int end, boolean atEOF) throws ScanException;
}
