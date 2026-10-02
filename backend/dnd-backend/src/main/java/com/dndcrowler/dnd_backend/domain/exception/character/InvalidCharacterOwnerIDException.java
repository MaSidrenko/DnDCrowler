package com.dndcrowler.dnd_backend.domain.exception.character;

public class InvalidCharacterOwnerIDException extends IllegalArgumentException {

	public InvalidCharacterOwnerIDException() {
		super("Owner can`t be null");
	}
}
