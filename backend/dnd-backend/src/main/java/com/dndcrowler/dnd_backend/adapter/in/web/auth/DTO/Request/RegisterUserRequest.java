package com.dndcrowler.dnd_backend.adapter.in.web.auth.DTO.Request;

public record RegisterUserRequest(
		String userName,
		String email,
		String password) {
	@Override 
	public String toString() {
	return "RegisterUserRequest[username=" + userName
		+ ", email=" + email
		+ ", password=[REDACTED]"
		+ "]";
	}
}
