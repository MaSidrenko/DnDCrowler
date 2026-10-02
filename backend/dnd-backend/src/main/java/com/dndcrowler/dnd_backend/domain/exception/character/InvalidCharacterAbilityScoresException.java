package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterAbilityScoresException extends IllegalArgumentException{

	public InvalidCharacterAbilityScoresException() {
		super("Ability can`t be null");
	}
	
}
