package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserEmailPatternException extends IllegalArgumentException {

	public InvalidUserEmailPatternException() {
		super("Input a correct format email");
	}
	
}
