package com.dndcrowler.dnd_backend.application.service.User;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.User.LoginUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.EmailNotVerifiedException;
import com.dndcrowler.dnd_backend.application.service.exception.InvalidCredentialsException;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class LoginUserServiceTest {
	@Mock
	UserRepositoryPort userRepositoryPort;
	@Mock
	PasswordHasherPort passwordHasherPort;
	@Mock
	PasswordCredentialsRepositoryPort passwordCredentialsRepositoryPort;

	@InjectMocks
	LoginUserService service;

	@Test
	void returnsUserWhenCredentialsAreValidAndEmailIsVerified() {
		Instant now = Instant.now();
		LoginUserCommand command = new LoginUserCommand("alice@example.com", "secret123");
		User verifiedUser = new User(1L, "Alice", command.email(), UserRole.USER, now, now, now);
		when(userRepositoryPort.findByEmail(command.email()))
				.thenReturn(Optional.of(verifiedUser));
		PasswordCredentials passwordCredentials = new PasswordCredentials(verifiedUser.id(), "hashed-password", now);
		when(passwordCredentialsRepositoryPort.findByUserId(verifiedUser.id()))
				.thenReturn(Optional.of(passwordCredentials));

		when(passwordHasherPort.matches(command.password(), passwordCredentials.passwordHash()))
				.thenReturn(true);

		User result = service.loginUser(command);
		assertEquals(verifiedUser, result);
	}

	@Test
	void throwsInvalidCredentialsWhenEmailIsNotFound() {
		LoginUserCommand command = new LoginUserCommand("alice@example.com", "secret123");
		when(userRepositoryPort.findByEmail(command.email()))
				.thenReturn(Optional.empty());

		assertThrows(InvalidCredentialsException.class,
				() -> service.loginUser(command));
		verifyNoInteractions(passwordCredentialsRepositoryPort, passwordHasherPort);
	}

	@Test
	void throwsInvalidCredentialsWhenPasswordCredentialsAreNotFound() {
		Instant now = Instant.now();
		LoginUserCommand command = new LoginUserCommand("alice@example.com", "secret123");
		User verifiedUser = new User(1L, "Alice", command.email(), UserRole.USER, now, now, now);
		when(userRepositoryPort.findByEmail(command.email()))
				.thenReturn(Optional.of(verifiedUser));

		when(passwordCredentialsRepositoryPort.findByUserId(verifiedUser.id()))
				.thenReturn(Optional.empty());

		assertThrows(InvalidCredentialsException.class,
				() -> service.loginUser(command));
		verifyNoInteractions(passwordHasherPort);
	}

	@Test
	void throwsInvalidCredentialsWhenPasswordDoesNotMatch() {
		Instant now = Instant.now();
		LoginUserCommand command = new LoginUserCommand("test@test.com", "wrongpassword");
		User unverifiedUser = new User(1L, "Test User", command.email(), UserRole.USER, now, now, now);
		when(userRepositoryPort.findByEmail(command.email()))
				.thenReturn(Optional.of(unverifiedUser));
		PasswordCredentials passwordCredentials = new PasswordCredentials(unverifiedUser.id(), "hashed-password", now);
		when(passwordCredentialsRepositoryPort.findByUserId(unverifiedUser.id()))
				.thenReturn(Optional.of(passwordCredentials));

		when(passwordHasherPort.matches(command.password(), passwordCredentials.passwordHash()))
				.thenReturn(false);

		assertThrows(InvalidCredentialsException.class,
				() -> service.loginUser(command));
	}

	@Test
	void throwsEmailNotVerifiedWhenEmailIsNotVerified() {
		Instant now = Instant.now();
		LoginUserCommand command = new LoginUserCommand("test@test.com", "secret123");
		User verifiedUser = new User(1L, "Test User", command.email(), UserRole.USER, now, now, null);

		when(userRepositoryPort.findByEmail(command.email()))
				.thenReturn(Optional.of(verifiedUser));
		PasswordCredentials passwordCredentials = new PasswordCredentials(verifiedUser.id(), "hashed-password", now);
		when(passwordCredentialsRepositoryPort.findByUserId(verifiedUser.id()))
				.thenReturn(Optional.of(passwordCredentials));

		when(passwordHasherPort.matches(command.password(), passwordCredentials.passwordHash()))
				.thenReturn(true);

		assertThrows(EmailNotVerifiedException.class,
				() -> service.loginUser(command));

		verify(passwordHasherPort)
				.matches(command.password(), passwordCredentials.passwordHash());
	}
}
