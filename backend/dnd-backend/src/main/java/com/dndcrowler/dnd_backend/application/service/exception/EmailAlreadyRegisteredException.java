package com.dndcrowler.dnd_backend.application.service.exception;

public class EmailAlreadyRegisteredException extends IllegalArgumentException {

	public EmailAlreadyRegisteredException() {
		super("Email already registered. Please use a different email.");
	}
	
}
