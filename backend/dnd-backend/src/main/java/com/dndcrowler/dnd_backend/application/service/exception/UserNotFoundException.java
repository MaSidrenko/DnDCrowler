package com.dndcrowler.dnd_backend.application.service.exception;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException(Long userId) {
		super("User not found: id=" + userId);
	}
}
