package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidArgumentScoreException extends IllegalArgumentException {

	public InvalidArgumentScoreException() {
		super("Stat` can`t be less than 1");
	}
	
}
