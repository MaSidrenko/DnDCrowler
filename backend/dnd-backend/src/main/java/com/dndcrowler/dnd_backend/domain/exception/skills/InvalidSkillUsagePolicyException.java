package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidSkillUsagePolicyException extends IllegalArgumentException {

	public InvalidSkillUsagePolicyException() {
		super("Skill usage policy can`t be empty");
	}
	
}
