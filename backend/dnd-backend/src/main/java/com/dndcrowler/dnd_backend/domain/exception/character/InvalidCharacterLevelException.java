package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterLevelException extends IllegalArgumentException {

	public InvalidCharacterLevelException() {
		super("Character level must be between 1 and 20");
	}
	
}
