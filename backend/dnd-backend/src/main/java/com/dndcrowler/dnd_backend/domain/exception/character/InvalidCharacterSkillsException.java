package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterSkillsException extends IllegalArgumentException {

	public InvalidCharacterSkillsException() {
		super("Skill can`t be empty");
	}
	
}
