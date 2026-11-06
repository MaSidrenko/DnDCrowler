package com.dndcrowler.dnd_backend.application.port.in.command.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.dndcrowler.dnd_backend.application.exception.InvalidLoginEmailException;
import com.dndcrowler.dnd_backend.application.exception.InvalidLoginPasswordException;

public class LoginUserCommandTest {
	@Test
	void emptyEmailThrowsException() {
		assertThrows(InvalidLoginEmailException.class,
				() -> new LoginUserCommand("", "secret123"));
	}

	@Test
	void nullEmailThrowsException() {
		assertThrows(InvalidLoginEmailException.class,
				() -> new LoginUserCommand(null, "secret123"));
	}

	@Test
	void blankEmailThrowsException() {
		assertThrows(InvalidLoginEmailException.class,
				() -> new LoginUserCommand("   ", "secret123"));
	}

	@Test
	void emptyPasswordThrowsException() {
		assertThrows(InvalidLoginPasswordException.class,
				() -> new LoginUserCommand("test@test.com", ""));
	}

	@Test
	void nullPasswordThrowsException() {
		assertThrows(InvalidLoginPasswordException.class,
				() -> new LoginUserCommand("test@test.com", null));
	}

	@Test
	void blankPasswordThrowsException() {
		assertThrows(InvalidLoginPasswordException.class,
				() -> new LoginUserCommand("test@test.com", "   "));
	}

	@Test
	void toStringDoesNotExposePassword() {
		LoginUserCommand command = new LoginUserCommand("test@test.com", "secret123");

		assertFalse(command.toString().contains("secret123"));
		assertTrue(command.toString().contains("[REDACTED]"));
	}

}
