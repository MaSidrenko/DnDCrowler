package com.dndcrowler.dnd_backend.adapter.out.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.dndcrowler.dnd_backend.application.port.out.Email.VerificationEmailSenderPort;

@Component 
public class SmtpVerificationEmailSenderAdapter implements VerificationEmailSenderPort {
	private static final Logger log = LoggerFactory.getLogger(SmtpVerificationEmailSenderAdapter.class);

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

		try {
			mailSender.send(message);
		} catch (MailException exception) {
			log.error(
				"Verification email delivery failed for {}",
				maskEmail(email)
			);
		}
	}

	private static String maskEmail(String email) {
		int separator = email.indexOf("@");

		if (separator <= 1) {
			return "***";
		}

		return email.charAt(0) + "***" + email.substring(separator);
	}

}
