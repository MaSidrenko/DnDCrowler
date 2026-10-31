package com.dndcrowler.dnd_backend.application.service.Email;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.ResendEmailVerificationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;
import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;
import com.dndcrowler.dnd_backend.domain.model.User.User;

import jakarta.transaction.Transactional;

@Service
public class ResendEmailVerificationService implements ResendEmailVerificationUseCase {
	private static final Logger log = LoggerFactory.getLogger(ResendEmailVerificationService.class);

	private final UserRepositoryPort userRepository;
	private final IssueEmailVerificationService issue;
	private final EmailVerificationRepositoryPort emailPort;

	public ResendEmailVerificationService(
		UserRepositoryPort userRepository, 
		IssueEmailVerificationService issue,
		EmailVerificationRepositoryPort emailPort
	) {
		this.userRepository = userRepository;
		this.issue = issue;
		this.emailPort = emailPort;
	}

	@Override
	@Transactional
	public void resend(ResendEmailVerificationCommand command) {
		Optional<User> optionalUser = userRepository.findByEmailForUpdate(command.email());

		if (optionalUser.isEmpty()) {
			log.debug("Verification resend ignored: user not found");
			return;
		}

		User user = optionalUser.get();

		if (user.emailVerifiedAt() != null) {
			log.debug("Verification resend ignored: email already verified, userId={}", user.id());
			return;
		}

		Optional<EmailVerificationToken> currentToken = emailPort.findCurrentByEmail(user.email());
		Instant now = Instant.now();

		if (currentToken.isPresent()
				&& currentToken.get().createdAt().plusSeconds(60).isAfter(now)) {
			log.debug("Verification resend ignored: cooldown is active, userId={}", user.id());
			return;
		}

		issue.issue(user.id(), user.email());

		log.info("Email verification code reissued: userId={}", user.id());
	}
}
