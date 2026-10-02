package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidSkillActivationTypeException extends IllegalArgumentException {

	public InvalidSkillActivationTypeException() {
		super("Skill activation can`t be empty");
	}
	
}
