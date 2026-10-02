package com.dndcrowler.dnd_backend.domain.exception.skills;

public class InvalidStatBonusStatException extends IllegalArgumentException {

	public InvalidStatBonusStatException() {
		super("Stat can`t be empty");
	}
	
}
