package com.dndcrowler.dnd_backend.application.service.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.User.RegisterUserCommand;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordCredentialsRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.Email.IssueEmailVerificationService;
import com.dndcrowler.dnd_backend.application.service.exception.EmailAlreadyRegisteredException;
import com.dndcrowler.dnd_backend.domain.model.Password.PasswordCredentials;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class RegisterUserServiceTest {

	@Mock
	UserRepositoryPort userRepositoryPort;
	@Mock
	PasswordHasherPort passwordHasherPort;
	@Mock
	PasswordCredentialsRepositoryPort credentialsRepositoryPort;
	@Mock
	IssueEmailVerificationService issueEmailVerificatioNService;

	@InjectMocks
	RegisterUserService service;

	@Test
	void registersUserAndSavesHashedPassword() {
		RegisterUserCommand command = new RegisterUserCommand("alice", "alice@example.com", "secret123");
		when(passwordHasherPort.hashPassword("secret123")).thenReturn("hashed-secret");
		when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);

			return new User(42L, user.username(), user.email(),
					user.role(), user.createdAt(), user.updatedAt(),
					user.emailVerifiedAt());
		});

		User result = service.registerUser(command);

		verify(issueEmailVerificatioNService).issue(42L, "alice@example.com");

		assertEquals(42L, result.id());
		assertEquals(UserRole.USER, result.role());
		verify(passwordHasherPort).hashPassword("secret123");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(userCaptor.capture());
		User userToSave = userCaptor.getValue();
		assertEquals("alice", userToSave.username());
		assertEquals("alice@example.com", userToSave.email());
		assertEquals(UserRole.USER, userToSave.role());

		ArgumentCaptor<PasswordCredentials> captor = ArgumentCaptor.forClass(PasswordCredentials.class);
		verify(credentialsRepositoryPort).save(captor.capture());
		assertEquals(42L, captor.getValue().userId());
		assertEquals("hashed-secret", captor.getValue().passwordHash());
		assertNotNull(captor.getValue().passwordChangedAt());
	}

	@Test
	void rejectsAlreadyRegisteredEmailWithoutSavingAnything() {
		RegisterUserCommand command = new RegisterUserCommand("alice", "alice@example.com", "secret123");
		when(userRepositoryPort.existsByEmail("alice@example.com")).thenReturn(true);



		assertThrows(EmailAlreadyRegisteredException.class,
				() -> service.registerUser(command));

		verify(userRepositoryPort, never()).save(any());
		verifyNoInteractions(passwordHasherPort, credentialsRepositoryPort, issueEmailVerificatioNService);
	}
}
