package com.dndcrowler.dnd_backend.application.service.User;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.Email.ConfirmEmailService;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationTest {

	@Mock
	EmailVerificationRepositoryPort emailPort;
	@Mock
	VerificationCodeHasherPort verificationPort;
	@Mock
	UserRepositoryPort userPort;

	@InjectMocks
	ConfirmEmailService service;

	@Test
	void trueEmailVerificationCode() {
		Instant now = Instant.now();
		EmailVerificationToken token = new EmailVerificationToken(
				10L, 1L, "example@exmpl.com", "hash_token", now.plusSeconds(5 * 60), null);
		User user = new User(1L, "Maxim", "example@exmpl.com", UserRole.USER, now, now, null);

		ConfirmEmailCommand command = new ConfirmEmailCommand("example@exmpl.com", "123456");
		when(emailPort.findCurrentByEmail("example@exmpl.com"))
				.thenReturn(Optional.of(token));
		when(verificationPort.matches("123456", "hash_token"))
				.thenReturn(true);
		when(userPort.findById(token.userId()))
				.thenReturn(Optional.of(user));

		boolean result = service.confirmEmail(command);

		assertTrue(result);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userPort).save(userCaptor.capture());

		assertNotNull(userCaptor.getValue().emailVerifiedAt());
		assertEquals(user.id(), userCaptor.getValue().id());

		ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);

		verify(emailPort).save(tokenCaptor.capture());

		assertNotNull(tokenCaptor.getValue().usedAt());
		assertEquals(token.id(), tokenCaptor.getValue().id());

		verify(verificationPort).matches("123456", "hash_token");
	}

	@Test
	void falseEmailVerificationCode() {
		EmailVerificationToken token = EmailVerificationToken.create(2L, "example2@exmpl.com", "hash_token",
				Instant.now().plusSeconds(5 * 60));

		ConfirmEmailCommand command = new ConfirmEmailCommand("example2@exmpl.com", "123456");
		when(emailPort.findCurrentByEmail("example2@exmpl.com"))
				.thenReturn(Optional.of(token));
		when(verificationPort.matches("123456", "hash_token"))
				.thenReturn(false);

		boolean result = service.confirmEmail(command);

		assertFalse(result);
		verify(verificationPort).matches("123456", "hash_token");

		verifyNoInteractions(userPort);

		verify(emailPort, never())
				.save(any(EmailVerificationToken.class));
	}

	@Test
	void expiresEmailVerificationCode() {
		EmailVerificationToken token = EmailVerificationToken.create(2L, "example3@exmpl.com", "hash_token",
				Instant.now().minusSeconds(5 * 60));

		ConfirmEmailCommand command = new ConfirmEmailCommand("example3@exmpl.com", "123456");

		when(emailPort.findCurrentByEmail("example3@exmpl.com"))
				.thenReturn(Optional.of(token));

		boolean result = service.confirmEmail(command);

		assertFalse(result);
		verify(emailPort).findCurrentByEmail("example3@exmpl.com");

		verifyNoInteractions(verificationPort);
		verifyNoInteractions(userPort);

		verify(emailPort, never())
				.save(any(EmailVerificationToken.class));
	}

	@Test
	void usedEmailVerificationCode() {
		Instant now = Instant.now();
		EmailVerificationToken token = new EmailVerificationToken(
				10L, 1L, "example4@exmpl.com",
				"hash_token", now.plusSeconds(5 * 60), null);
		EmailVerificationToken usedToken = token.markAsUsed();

		ConfirmEmailCommand command = new ConfirmEmailCommand("example4@exmpl.com", "123456");
		when(emailPort.findCurrentByEmail("example4@exmpl.com"))
				.thenReturn(Optional.of(usedToken));

		boolean result = service.confirmEmail(command);

		assertFalse(result);

		verify(emailPort).findCurrentByEmail("example4@exmpl.com");

		verifyNoInteractions(verificationPort);
		verifyNoInteractions(userPort);

		verify(emailPort, never())
				.save(any(EmailVerificationToken.class));
	}
}