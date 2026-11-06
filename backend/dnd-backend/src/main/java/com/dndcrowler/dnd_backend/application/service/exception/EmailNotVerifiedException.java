package com.dndcrowler.dnd_backend.application.service.exception;

public class EmailNotVerifiedException extends RuntimeException {
	public EmailNotVerifiedException() {
		super("Email address is not verified");
	}
}
