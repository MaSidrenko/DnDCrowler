package com.dndcrowler.dnd_backend.application.port.out.Email;

public interface VerificationEmailSenderPort {
	void sender(String email, String code);
}
