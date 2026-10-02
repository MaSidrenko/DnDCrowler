package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterRaceArgumentException extends IllegalArgumentException {

	public InvalidCharacterRaceArgumentException() {
		super("Race can`t be null");
	}
	
}
