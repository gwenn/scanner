package org.bufio;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ScanException extends IOException {
	ScanException(@Nonnull String message) {
		super(message);
	}
}
