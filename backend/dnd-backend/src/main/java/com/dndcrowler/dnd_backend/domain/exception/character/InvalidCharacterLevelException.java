package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterLevelException extends IllegalArgumentException {

	public InvalidCharacterLevelException() {
		super("Start level can`t be less than 1");
	}
	
}
