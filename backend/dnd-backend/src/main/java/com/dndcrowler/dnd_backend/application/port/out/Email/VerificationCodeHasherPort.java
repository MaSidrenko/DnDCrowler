package com.dndcrowler.dnd_backend.application.port.out.Email;

public interface VerificationCodeHasherPort {
	String hash(String code);

	boolean matches(String rawCode, String storedHash);
}
