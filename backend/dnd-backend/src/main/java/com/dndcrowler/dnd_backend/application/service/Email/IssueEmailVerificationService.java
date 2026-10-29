package com.dndcrowler.dnd_backend.application.service.Email;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;

import jakarta.transaction.Transactional;

@Service
public class IssueEmailVerificationService {
	private static final SecureRandom RANDOM = new SecureRandom();

	private final VerificationCodeHasherPort hasher;
	private final EmailVerificationRepositoryPort tokens;
	private final ApplicationEventPublisher events;

	public IssueEmailVerificationService(VerificationCodeHasherPort hasher, 
			EmailVerificationRepositoryPort tokens,
			ApplicationEventPublisher events) {
		this.hasher = hasher;
		this.tokens = tokens;
		this.events = events;
	}

	@Transactional
	public void issue(Long userId, String email) {
		String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));

		EmailVerificationToken token = EmailVerificationToken.create(
			userId,
			email,
			hasher.hash(code),
			Instant.now().plus(Duration.ofMinutes(5))
		);
		
		tokens.save(token);

		events.publishEvent(new VerificationCodeIssuedEvent(email, code));
	}
}