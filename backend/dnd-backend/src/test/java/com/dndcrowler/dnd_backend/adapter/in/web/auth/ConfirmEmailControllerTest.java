package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dndcrowler.dnd_backend.application.port.in.ConfirmEmailUseCase;
import com.dndcrowler.dnd_backend.application.port.in.ResendEmailVerificationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ConfirmEmailCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Email.ResendEmailVerificationCommand;

@ExtendWith(MockitoExtension.class)
public class ConfirmEmailControllerTest {
	@Mock
	private ConfirmEmailUseCase confirmUseCase;
	@Mock
	private ResendEmailVerificationUseCase resendUseCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ConfirmEmailController(confirmUseCase, resendUseCase))
				.build();
	}

	@Test
	void confirmsEmail() throws Exception {
		when(confirmUseCase.confirmEmail(any(ConfirmEmailCommand.class)))
				.thenReturn(true);

		mockMvc.perform(post("/api/auth/email/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
								{
									"email": "alice@example.com",
									"code": "123456"
								}
								"""))
				.andExpect(status().isNoContent());

		ArgumentCaptor<ConfirmEmailCommand> commandCaptor = ArgumentCaptor.forClass(ConfirmEmailCommand.class);

		verify(confirmUseCase).confirmEmail(commandCaptor.capture());

		assertEquals("alice@example.com", commandCaptor.getValue().email());
		assertEquals("123456", commandCaptor.getValue().code());
	}

	@Test
	void rejectsUnsuccessfulConfirmation() throws Exception {
		when(confirmUseCase.confirmEmail(any(ConfirmEmailCommand.class)))
				.thenReturn(false);

		mockMvc.perform(post("/api/auth/email/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
									{
										"email": "alice@example.com",
										"code": "123456"
									}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/auth/email/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
									{
										"email": "invalid-email",
										"code": "123456"
									}
								"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(confirmUseCase);
	}

	@Test
	void rejectsInvalidCodeFormat() throws Exception {
		mockMvc.perform(post("/api/auth/email/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
									{
										"email": "alice@example.com",
										"code": "1234"
									}
								"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(confirmUseCase);
	}

	@Test
	void resendVerificationEmail() throws Exception {
		mockMvc.perform(post("/api/auth/email/resend")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
						"""
									{
										"email": "alice@example.com"
									}
								"""))
				.andExpect(status().isNoContent());

		ArgumentCaptor<ResendEmailVerificationCommand> commandCaptor = ArgumentCaptor
				.forClass(ResendEmailVerificationCommand.class);

		verify(resendUseCase).resend(commandCaptor.capture());

		assertEquals("alice@example.com", commandCaptor.getValue().email());
	}

	@Test
	void rejectsInvalidResendEmail() throws Exception {
		mockMvc.perform(post("/api/auth/email/resend")
			.contentType(MediaType.APPLICATION_JSON)
			.content(
				"""
					{
						"email": "invalid_email"
					}
				"""
			)
		).andExpect(status().isBadRequest());

		verifyNoInteractions(resendUseCase);
	}
}
