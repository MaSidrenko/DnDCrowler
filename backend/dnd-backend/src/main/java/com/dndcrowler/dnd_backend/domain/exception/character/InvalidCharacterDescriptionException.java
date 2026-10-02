package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterDescriptionException extends IllegalArgumentException {

	public InvalidCharacterDescriptionException() {
		super("Description can`t be empty or null");
	}
	
}
