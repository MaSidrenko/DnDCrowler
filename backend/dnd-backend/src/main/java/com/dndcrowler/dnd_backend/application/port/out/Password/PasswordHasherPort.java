package com.dndcrowler.dnd_backend.application.port.out.Password;

public interface PasswordHasherPort {
	String hashPassword(String password);
}
