package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenUserIdException extends IllegalArgumentException {

	public InvalidEmailTokenUserIdException() {
		super("Id can`t be empty");
	}
	
}
