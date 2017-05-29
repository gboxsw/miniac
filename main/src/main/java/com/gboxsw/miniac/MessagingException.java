package com.gboxsw.miniac;

/**
 * Thrown when subscribe failed.
 */
public class MessagingException extends RuntimeException {

	private static final long serialVersionUID = -8920791406589614471L;

	public MessagingException() {
		super();
	}

	public MessagingException(String message, Throwable cause) {
		super(message, cause);
	}

	public MessagingException(String message) {
		super(message);
	}

	public MessagingException(Throwable cause) {
		super(cause);
	}
}
