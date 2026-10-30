package com.dndcrowler.dnd_backend.application.port.in;

import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;

public interface ResendEmailVerificationUseCase {
	void resend(ResendEmailVerificationCommand command);
}
