package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserEmailException extends IllegalArgumentException {

	public InvalidUserEmailException() {
		super("Email can`t be empty");
	}
	
}
