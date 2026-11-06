package com.dndcrowler.dnd_backend.application.service.exception;

public class InvalidCredentialsException extends RuntimeException {
	public InvalidCredentialsException() {
		super("Invalid email or password");
	}
}
