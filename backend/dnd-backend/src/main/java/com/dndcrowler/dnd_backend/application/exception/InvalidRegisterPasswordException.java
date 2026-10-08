package com.dndcrowler.dnd_backend.application.exception;

public class InvalidRegisterPasswordException extends IllegalArgumentException {
	public InvalidRegisterPasswordException() {
		super("Password cannot be null or blank");
	}
}
