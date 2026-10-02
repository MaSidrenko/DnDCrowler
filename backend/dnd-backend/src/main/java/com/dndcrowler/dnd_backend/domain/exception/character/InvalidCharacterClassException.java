package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterClassException extends IllegalArgumentException {

	public InvalidCharacterClassException() {
		super("Character class can`t be empty");
	}
	
}
