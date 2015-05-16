package com.koala.game.exception;

public class KGameServerException extends Exception{

	private static final long serialVersionUID = 1L;

	public KGameServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public KGameServerException(String message) {
		super(message);
	}

	public KGameServerException(Throwable cause) {
		super(cause);
	}

}
