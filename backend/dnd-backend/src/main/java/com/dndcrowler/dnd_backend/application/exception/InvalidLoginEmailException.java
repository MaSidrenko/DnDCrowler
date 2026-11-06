package com.dndcrowler.dnd_backend.application.exception;

public class InvalidLoginEmailException extends IllegalArgumentException {
	public InvalidLoginEmailException() {
		super("Email cannot be null or blank");
	}
}
