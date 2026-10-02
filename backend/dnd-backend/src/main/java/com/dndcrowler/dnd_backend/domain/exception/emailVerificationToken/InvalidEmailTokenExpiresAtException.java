package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenExpiresAtException extends IllegalArgumentException {

	public InvalidEmailTokenExpiresAtException() {
		super("Expires at can`t be null!");
	}
	
}
