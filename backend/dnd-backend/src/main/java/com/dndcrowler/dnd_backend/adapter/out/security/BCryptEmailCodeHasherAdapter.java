package com.dndcrowler.dnd_backend.adapter.out.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationCodeHasherPort;

@Component
public class BCryptEmailCodeHasherAdapter implements VerificationCodeHasherPort {
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hash(String code) {
		return encoder.encode(code);
	}

	@Override
	public boolean matches(String rawCode, String storedHash) {
		return encoder.matches(rawCode, storedHash);
	}
}
