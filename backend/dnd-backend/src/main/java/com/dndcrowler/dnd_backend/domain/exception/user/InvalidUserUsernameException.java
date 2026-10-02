package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserUsernameException extends IllegalArgumentException {

	public InvalidUserUsernameException() {
		super("Username can`t be empty");
	}
	
}
