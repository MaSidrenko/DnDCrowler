package com.dndcrowler.dnd_backend.domain.exception.user;

public class InvalidUserCreatedAtException extends IllegalArgumentException {

	public InvalidUserCreatedAtException() {
		super("User created at can`t be null!");
	}
	
}
