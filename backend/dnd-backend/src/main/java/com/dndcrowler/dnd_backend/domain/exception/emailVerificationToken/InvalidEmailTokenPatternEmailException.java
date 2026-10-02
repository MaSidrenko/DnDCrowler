package com.dndcrowler.dnd_backend.domain.exception.emailVerificationToken;

public class InvalidEmailTokenPatternEmailException extends IllegalArgumentException {

	public InvalidEmailTokenPatternEmailException() {
		super("Input a correct format email");
	}
	
}
