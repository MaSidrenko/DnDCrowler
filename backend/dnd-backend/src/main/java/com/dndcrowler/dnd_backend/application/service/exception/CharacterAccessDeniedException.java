package com.dndcrowler.dnd_backend.application.service.exception;

public class CharacterAccessDeniedException extends RuntimeException {
	public CharacterAccessDeniedException() {
		super("You do not have permission to modify this character");
	}
}
