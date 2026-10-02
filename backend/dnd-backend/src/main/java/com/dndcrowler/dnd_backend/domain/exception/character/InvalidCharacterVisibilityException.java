package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterVisibilityException extends IllegalArgumentException {

	public InvalidCharacterVisibilityException() {
		super("Character visibility can`t be empty");
	}
	
}
