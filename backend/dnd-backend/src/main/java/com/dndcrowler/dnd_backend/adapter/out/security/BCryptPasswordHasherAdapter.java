package com.dndcrowler.dnd_backend.adapter.out.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.dndcrowler.dnd_backend.application.port.out.Password.PasswordHasherPort;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hashPassword(String password) {
		return encoder.encode(password);
	}

}
