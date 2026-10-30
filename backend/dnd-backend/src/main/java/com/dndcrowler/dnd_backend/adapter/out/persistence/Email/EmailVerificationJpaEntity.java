package com.dndcrowler.dnd_backend.adapter.out.persistence.Email;

import java.time.Instant;

import com.dndcrowler.dnd_backend.domain.model.EmailVerificationToken.EmailVerificationToken;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification")
public class EmailVerificationJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private Long userId;

	private String email;
	private String tokenHash;
	private Instant createdAt;
	private Instant expiresAt;
	private Instant usedAt;

	protected EmailVerificationJpaEntity() {
	}

	public EmailVerificationJpaEntity(EmailVerificationToken token) {
		this.id = token.id();
		this.userId = token.userId();
		this.email = token.email();
		this.tokenHash = token.tokenHash();
		this.createdAt = token.createdAt();
		this.expiresAt = token.expiresAt();
		this.usedAt = token.usedAt();
	}

	public EmailVerificationToken toDomain() {
		return new EmailVerificationToken(id, userId, email, tokenHash,createdAt, expiresAt, usedAt);
	}
}
