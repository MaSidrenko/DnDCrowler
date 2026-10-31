package com.dndcrowler.dnd_backend.application.service.Email;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;
import com.dndcrowler.dnd_backend.domain.model.User.User;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
public class ResendEmailVerificationServiceTest {

	@Mock
	UserRepositoryPort userRepository;
	@Mock
	EmailVerificationRepositoryPort emailRepository;
	@Mock
	IssueEmailVerificationService issueService;

	@InjectMocks
	ResendEmailVerificationService service;

	@Test
	void findByEmailReturnEmptyUser() {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand("nullref@example.com");

		when(userRepository.findByEmailForUpdate(command.email()))
				.thenReturn(Optional.empty());

		service.resend(command);

		verify(userRepository).findByEmailForUpdate(command.email());

		verifyNoInteractions(emailRepository, issueService);
	}

	@Test
	void emailAlreadyConfirm() {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand("existing_email@example.com");

		Instant now = Instant.now();

		User user = new User(
				1L,
				"exisiting_user",
				command.email(),
				UserRole.USER,
				now,
				now,
				now);

		when(userRepository.findByEmailForUpdate(command.email()))
				.thenReturn(Optional.of(user));

		service.resend(command);

		verify(userRepository).findByEmailForUpdate(command.email());
		verifyNoInteractions(emailRepository, issueService);
	}

	@Test
	void pauseNotFinished() {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand("spam_email@example.com");
		Instant now = Instant.now();

		User user = new User(
				1L,
				"spam_user",
				command.email(),
				UserRole.USER,
				now,
				now,
				null);

		EmailVerificationToken token = new EmailVerificationToken(
				1L,
				user.id(),
				command.email(),
				"hash",
				now,
				now.plusSeconds(60 * 5),
				null);

		when(userRepository.findByEmailForUpdate(command.email()))
				.thenReturn(Optional.of(user));
		when(emailRepository.findCurrentByEmail(user.email()))
				.thenReturn(Optional.of(token));

		service.resend(command);

		verify(userRepository).findByEmailForUpdate(command.email());
		verify(emailRepository).findCurrentByEmail(command.email());

		verifyNoInteractions(issueService);
	}

	@Test
	void pauseFinished() {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand("test-email@example.com");
		Instant nowPluseMinute = Instant.now().minusSeconds(61);
		Instant now = Instant.now();

		User user = new User(
				1L,
				"test-user",
				command.email(),
				UserRole.USER,
				now,
				now,
				null);

		EmailVerificationToken token = new EmailVerificationToken(
				1L,
				user.id(),
				command.email(),
				"hash",
				nowPluseMinute,
				now.plusSeconds(60 * 5),
				null);

		when(userRepository.findByEmailForUpdate(command.email()))
				.thenReturn(Optional.of(user));
		when(emailRepository.findCurrentByEmail(user.email()))
				.thenReturn(Optional.of(token));

		service.resend(command);
		verify(userRepository).findByEmailForUpdate(command.email());
		verify(emailRepository).findCurrentByEmail(command.email());
		verify(issueService).issue(user.id(), user.email());
	}

	@Test
	void issuesCodeWhenPreviousTokenDoesNotExist() {
		ResendEmailVerificationCommand command = new ResendEmailVerificationCommand("without-token@example.com");
		Instant now = Instant.now();
		User user = new User(
				1L,
				"test-user",
				command.email(),
				UserRole.USER,
				now,
				now,
				null);

		when(userRepository.findByEmailForUpdate(command.email()))
				.thenReturn(Optional.of(user));

		when(emailRepository.findCurrentByEmail(command.email()))
				.thenReturn(Optional.empty());

		service.resend(command);

		verify(userRepository).findByEmailForUpdate(command.email());
		verify(emailRepository).findCurrentByEmail(user.email());
		verify(issueService).issue(user.id(), user.email());
	}
}
