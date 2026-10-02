package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidSkillEffectsException extends IllegalArgumentException {

	public InvalidSkillEffectsException() {
		super("Effects can`t be empty");
	}
	
}
