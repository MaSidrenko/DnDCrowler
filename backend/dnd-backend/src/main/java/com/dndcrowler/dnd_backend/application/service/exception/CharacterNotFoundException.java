package com.dndcrowler.dnd_backend.application.service.exception;

public class CharacterNotFoundException extends RuntimeException {
	public CharacterNotFoundException(Long characterId) {
		super("Character not found: id=" + characterId);
	}
}
