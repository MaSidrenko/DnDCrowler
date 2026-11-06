package com.dndcrowler.dnd_backend.application.exception;

public class InvalidLoginPasswordException extends IllegalArgumentException {
	public InvalidLoginPasswordException() {
		super("Password cannot be null or blank");
	}
}
