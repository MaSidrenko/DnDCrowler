package com.dndcrowler.dnd_backend.application.service.Email;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;

@Component 
public class VerificationCodeIssuedListener {
	private final VerificationEmailSenderPort emailSender;

	public VerificationCodeIssuedListener(VerificationEmailSenderPort emailSender) {
		this.emailSender = emailSender;
	}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCodeIssued(VerificationCodeIssuedEvent event) {
		emailSender.sender(event.email(), event.code());
	}
}
