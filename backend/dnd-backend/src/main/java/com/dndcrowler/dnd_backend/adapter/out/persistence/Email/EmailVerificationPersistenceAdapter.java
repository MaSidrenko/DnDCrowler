package com.dndcrowler.dnd_backend.adapter.out.persistence.Email;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.out.Email.EmailVerificationRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;

@Repository 
public class EmailVerificationPersistenceAdapter implements EmailVerificationRepositoryPort {
	private final SpringDataEmailVerificationRepository repository;

	public EmailVerificationPersistenceAdapter(SpringDataEmailVerificationRepository repository) {
		this.repository = repository;
	}

	@Override
	public EmailVerificationToken save(EmailVerificationToken token) {
		return repository.save(new EmailVerificationJpaEntity(token))
			.toDomain();
	}

	@Override
	public Optional<EmailVerificationToken> findCurrentByEmail(String email) {
		return repository.findFirstByEmailOrderByIdDesc(email).map(EmailVerificationJpaEntity::toDomain);
	}

}
