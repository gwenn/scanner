package org.bufio;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Port of Go Scanner in Java.
 */
public abstract class Scanner<T> implements Closeable, CharSequence {
	// The reader provided by the client.
	private /*final*/ Reader r;
	// Maximum size of a token
	private final int maxTokenSize;
	// Last token returned by split.
	private T token;
	// Buffer used as argument to split.
	private char[] buf;
	// First non-processed byte in buf.
	private int start;
	// End of data in buf.
	private int end;

	private boolean eof;

	protected Scanner(Reader r) {
		reset(r);
		maxTokenSize = 64 * 1024;
		buf = new char[4096]; // Plausible starting size; needn't be large.
	}

	/** Reuse this scanner with a new content. */
	protected void reset(Reader r) {
		if (r == null) {
			throw new IllegalArgumentException("null reader");
		}
		this.r = r;
		token = null;
		start = 0; end = 0;
		eof = false;
	}

	/** Advances the Scanner to the next token, which will then be
	 * available through the {@link #token} method.
	 * @return false when the scan stops, by reaching the end of the input.
	 */
	public boolean scan() throws IOException {
		// Loop until we have a token.
		while (true) {
			// See if we can get a token with what we already have.
			if (end > start || eof) {
				final int pstart = start;
				token = split(buf, start, end, eof);
				if (token != null) {
					return true;
				} else if (pstart != start) {
					continue;
				}
			}
			// We cannot generate a token with what we are holding.
			// If we've already hit EOF, we are done.
			if (eof) {
				start = 0;
				end = 0;
				return false;
			}
			read();
		}
	}

	private void read() throws IOException {
		// Must read more data.
		// First, shift data to beginning of buffer if there's lots of empty space
		// or space is needed.
		if (start > 0 && (end == buf.length || start > buf.length / 2)) {
			System.arraycopy(buf, start, buf, 0, end - start);
			end -= start;
			start = 0;
		}
		// Is the buffer full? If so, resize.
		if (end == buf.length) {
			if (buf.length >= maxTokenSize) {
				throw new ScanException("token too long");
			}
			int newSize = Math.min(buf.length * 2, maxTokenSize);
			char[] newBuf = new char[newSize];
			System.arraycopy(buf, start, newBuf, 0, end - start);
			buf = newBuf;
			end -= start;
			start = 0;
			return;
		}
		// Finally we can read some input.
		int n = r.read(buf, end, buf.length - end);
		if (n < 0) {
			eof = true;
		} else {
			end += n;
		}
	}

	/** The function to split the tokens. */
	protected abstract T split(char[] data, int start, int end, boolean atEOF) throws ScanException;

	/** @return The token returned by {@link #split} function */
	protected T token() {
		return token;
	}

	public char peek() throws IOException {
		while (true) {
			if (end > start) {
				return buf[start];
			}
			if (eof) {
				return 0;
			}
			read();
		}
	}

	public boolean atEndOfFile() {
		return eof;
	}

	@Override
	public void close() throws IOException {
		r.close();
	}

	/** Used by {@link #split} function to advance the input.
	 * @param n the number of bytes.
	 */
	protected void advance(int n) throws ScanException {
		if (n < 0) {
			throw new ScanException("SplitFunc returns negative advance count");
		}
		if (n > end) {
			throw new ScanException("SplitFunc returns advance count beyond input");
		}
		start = n;
	}

	/** @return Position of the first non-processed byte in buffer. */
	protected int position() {
		return start;
	}

	@Override
	public int length() {
		return end - start;
	}

	@Override
	public char charAt(int index) {
		if (index < 0 || index >= length()) {
			throw new IndexOutOfBoundsException("index: " + index);
		}
		return buf[start + index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if ((start < 0) || (end < 0) || (start > end) || (end > length())) {
			throw new IndexOutOfBoundsException();
		}
		return new String(buf, this.start + start, end - start);
	}

	@Override
	public String toString() {
		return new String(buf, start, end - start);
	}
}
