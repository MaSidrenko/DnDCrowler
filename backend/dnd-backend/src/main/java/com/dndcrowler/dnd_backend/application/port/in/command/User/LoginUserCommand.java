package com.dndcrowler.dnd_backend.application.port.in.command.User;

import com.dndcrowler.dnd_backend.application.exception.InvalidLoginEmailException;
import com.dndcrowler.dnd_backend.application.exception.InvalidLoginPasswordException;

public record LoginUserCommand(
	String email,
	String password
) {
	public LoginUserCommand {
		if(email == null || email.isBlank()) 
			throw new InvalidLoginEmailException();

		if(password == null || password.isBlank())
			throw new InvalidLoginPasswordException();
	}

	@Override 
	public String toString() {
		return "LoginUserCommand[email=" + email
		+ ", password=[REDACTED]"
		+ "]";
	}
}
