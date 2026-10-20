package com.dndcrowler.dnd_backend.application.port.in.command.Email;

public record ConfirmEmailCommand(String email, String code) {
	
}
