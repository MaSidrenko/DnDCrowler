package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterNameException extends IllegalArgumentException {

	public InvalidCharacterNameException() {
		super("Name can`t be empty or null");
	}
	
}
