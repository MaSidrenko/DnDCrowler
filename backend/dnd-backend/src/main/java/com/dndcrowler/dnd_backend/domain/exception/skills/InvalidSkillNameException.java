package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidSkillNameException extends IllegalArgumentException {

	public InvalidSkillNameException() {
		super("Skill name can`t be empty");
	}
	
}
