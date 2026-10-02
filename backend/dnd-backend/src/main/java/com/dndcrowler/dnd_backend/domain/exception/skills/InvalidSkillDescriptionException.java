package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidSkillDescriptionException extends IllegalArgumentException {

	public InvalidSkillDescriptionException() {
		super("Skill description can`t be empty");
	}
	
}
