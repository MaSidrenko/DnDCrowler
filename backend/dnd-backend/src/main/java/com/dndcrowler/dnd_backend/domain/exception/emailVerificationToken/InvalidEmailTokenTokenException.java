package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenTokenException extends IllegalArgumentException {

	public InvalidEmailTokenTokenException() {
		super("Token can`t be empty");
	}
	
}
