package org.bufio;

import java.io.IOException;

public class ScanException extends IOException {
	ScanException(String message) {
		super(message);
	}
}
