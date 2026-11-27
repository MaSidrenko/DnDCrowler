package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CampaignApplicationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationAlreadyExistsException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignApplicationNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterLevelNotAllowedException;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplicationStatus;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CampaignApplicationControllerTest {
	private static final Long USER_ID = 42L;
	private static final Long CAMPAIGN_ID = 100L;
	private static final Long APPLICATION_ID = 200L;
	private static final Long CHARACTER_ID = 300L;
	private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	CampaignApplicationUseCase useCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				USER_ID, "alice", "alice@example.com", UserRole.USER);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		mockMvc = MockMvcBuilders
				.standaloneSetup(new CampaignApplicationController(useCase))
				.setControllerAdvice(new CampaignApplicationExceptionHandler())
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void submitsApplicationForAuthenticatedUser() throws Exception {
		when(useCase.submit(any(SubmitCampaignApplicationCommand.class)))
				.thenReturn(application(CampaignApplicationStatus.PENDING));

		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "characterId": 300,
						  "message": "Let me join"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(APPLICATION_ID))
				.andExpect(jsonPath("$.campaignId").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$.applicantId").value(USER_ID))
				.andExpect(jsonPath("$.characterId").value(CHARACTER_ID))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.message").value("Let me join"));

		ArgumentCaptor<SubmitCampaignApplicationCommand> captor =
				ArgumentCaptor.forClass(SubmitCampaignApplicationCommand.class);
		verify(useCase).submit(captor.capture());
		assertEquals(USER_ID, captor.getValue().currentUserId());
		assertEquals(CAMPAIGN_ID, captor.getValue().campaignId());
		assertEquals(CHARACTER_ID, captor.getValue().characterId());
	}

	@Test
	void rejectsInvalidSubmitRequest() throws Exception {
		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "characterId": 0,
						  "message": "x"
						}
						"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	@Test
	void returnsApplicationsForAuthenticatedUser() throws Exception {
		when(useCase.getMyApplications(USER_ID))
				.thenReturn(List.of(application(CampaignApplicationStatus.PENDING)));

		mockMvc.perform(get("/api/campaign-applications/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(APPLICATION_ID))
				.andExpect(jsonPath("$[0].status").value("PENDING"));

		verify(useCase).getMyApplications(USER_ID);
	}

	@Test
	void returnsCampaignApplicationsToGameMaster() throws Exception {
		when(useCase.getCampaignApplications(CAMPAIGN_ID, USER_ID))
				.thenReturn(List.of(application(CampaignApplicationStatus.PENDING)));

		mockMvc.perform(get("/api/campaigns/{campaignId}/applications", CAMPAIGN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].campaignId").value(CAMPAIGN_ID));

		verify(useCase).getCampaignApplications(CAMPAIGN_ID, USER_ID);
	}

	@Test
	void acceptsApplication() throws Exception {
		when(useCase.accept(APPLICATION_ID, USER_ID))
				.thenReturn(application(CampaignApplicationStatus.ACCEPTED));

		mockMvc.perform(post("/api/campaign-applications/{applicationId}/accept", APPLICATION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"));

		verify(useCase).accept(APPLICATION_ID, USER_ID);
	}

	@Test
	void rejectsApplication() throws Exception {
		when(useCase.reject(APPLICATION_ID, USER_ID))
				.thenReturn(application(CampaignApplicationStatus.REJECTED));

		mockMvc.perform(post("/api/campaign-applications/{applicationId}/reject", APPLICATION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		verify(useCase).reject(APPLICATION_ID, USER_ID);
	}

	@Test
	void withdrawsApplication() throws Exception {
		when(useCase.withdraw(APPLICATION_ID, USER_ID))
				.thenReturn(application(CampaignApplicationStatus.WITHDRAWN));

		mockMvc.perform(post("/api/campaign-applications/{applicationId}/withdraw", APPLICATION_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WITHDRAWN"));

		verify(useCase).withdraw(APPLICATION_ID, USER_ID);
	}

	@Test
	void mapsApplicationNotFoundTo404() throws Exception {
		when(useCase.accept(APPLICATION_ID, USER_ID))
				.thenThrow(new CampaignApplicationNotFoundException(APPLICATION_ID));

		mockMvc.perform(post("/api/campaign-applications/{applicationId}/accept", APPLICATION_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(
						"Campaign application not found: id=200"));
	}

	@Test
	void mapsAccessDeniedTo403() throws Exception {
		when(useCase.withdraw(APPLICATION_ID, USER_ID))
				.thenThrow(new CampaignApplicationAccessDeniedException());

		mockMvc.perform(post("/api/campaign-applications/{applicationId}/withdraw", APPLICATION_ID))
				.andExpect(status().isForbidden());
	}

	@Test
	void mapsApplicationConflictTo409() throws Exception {
		when(useCase.submit(any(SubmitCampaignApplicationCommand.class)))
				.thenThrow(new CampaignApplicationAlreadyExistsException(CAMPAIGN_ID, USER_ID));

		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"characterId\":300}"))
				.andExpect(status().isConflict());
	}

	@Test
	void mapsInvalidCharacterLevelTo400() throws Exception {
		when(useCase.submit(any(SubmitCampaignApplicationCommand.class)))
				.thenThrow(new CharacterLevelNotAllowedException(CHARACTER_ID, 1, 3, 10));

		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"characterId\":300}"))
				.andExpect(status().isBadRequest());
	}

	private CampaignApplication application(CampaignApplicationStatus status) {
		return new CampaignApplication(
				APPLICATION_ID,
				CAMPAIGN_ID,
				USER_ID,
				CHARACTER_ID,
				status,
				"Let me join",
				TIME,
				TIME);
	}
}
