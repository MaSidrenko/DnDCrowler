package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenEmailException extends IllegalArgumentException {

	public InvalidEmailTokenEmailException() {
		super("Email can`t be empty");
	}
	
}
