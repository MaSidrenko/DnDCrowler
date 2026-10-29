package com.dndcrowler.dnd_backend.adapter.out.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;

@Component 
public class SmtpVerificationEmailSenderAdapter implements VerificationEmailSenderPort {
	private final JavaMailSender mailSender;
	private final String from;

	public SmtpVerificationEmailSenderAdapter(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override 
	public void sender(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();

		message.setFrom(from);
		message.setTo(email);
		message.setSubject("Подтверждение email");
		message.setText("Ваш код подтверждения: " + code + "\nКод действует 5 минут.");

		mailSender.send(message);
	}

}
