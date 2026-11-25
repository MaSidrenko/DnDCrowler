package com.dndcrowler.dnd_backend.adapter.in.web.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.dndcrowler.dnd_backend.application.port.in.CampaignUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.CreateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.UpdateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignLevelRangeException;
import com.dndcrowler.dnd_backend.domain.exception.campaign.InvalidCampaignStatusTransitionException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignSchedule;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {
	private static final Long OWNER_ID = 42L;
	private static final Long CAMPAIGN_ID = 100L;

	@Mock
	private CampaignUseCase useCase;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				OWNER_ID,
				"alice",
				"alice@example.com",
				UserRole.USER);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);

		mockMvc = MockMvcBuilders.standaloneSetup(new CampaignController(useCase))
				.setControllerAdvice(new CampaignExceptionHandler())
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsCampaignForAuthenticatedUser() throws Exception {
		when(useCase.createCampaign(any(CreateCampaignCommand.class)))
				.thenReturn(campaign(CampaignStatus.DRAFT));

		mockMvc.perform(post("/api/campaigns")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$.gameMasterId").value(OWNER_ID))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.schedule.dayOfWeek").value("SATURDAY"))
				.andExpect(jsonPath("$.schedule.startTime").value("19:30:00"))
				.andExpect(jsonPath("$.schedule.timeZone").value("Europe/Moscow"));

		ArgumentCaptor<CreateCampaignCommand> captor =
				ArgumentCaptor.forClass(CreateCampaignCommand.class);
		verify(useCase).createCampaign(captor.capture());
		assertEquals(OWNER_ID, captor.getValue().currentUserId());
		assertEquals("Curse of Strahd", captor.getValue().name());
		assertEquals(DayOfWeek.SATURDAY, captor.getValue().schedule().dayOfWeek());
		assertEquals(ZoneId.of("Europe/Moscow"), captor.getValue().schedule().timeZone());
	}

	@Test
	void updatesCampaignForAuthenticatedOwner() throws Exception {
		when(useCase.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
				.thenReturn(campaign(CampaignStatus.DRAFT));

		mockMvc.perform(put("/api/campaigns/{campaignId}", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CAMPAIGN_ID));

		ArgumentCaptor<UpdateCampaignCommand> captor =
				ArgumentCaptor.forClass(UpdateCampaignCommand.class);
		verify(useCase).updateCampaign(eq(CAMPAIGN_ID), captor.capture());
		assertEquals(OWNER_ID, captor.getValue().currentUserId());
		assertEquals(6, captor.getValue().maxPlayerCount());
	}

	@ParameterizedTest
	@EnumSource(StatusAction.class)
	void executesCampaignStatusAction(StatusAction action) throws Exception {
		stubStatusAction(action);

		mockMvc.perform(post("/api/campaigns/{campaignId}/{action}", CAMPAIGN_ID, action.path))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(action.resultStatus.name()));

		verifyStatusAction(action);
	}

	@Test
	void returnsCampaignsOwnedByAuthenticatedUser() throws Exception {
		when(useCase.getMyCampaigns(OWNER_ID))
				.thenReturn(List.of(campaign(CampaignStatus.DRAFT)));

		mockMvc.perform(get("/api/campaigns/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$[0].status").value("DRAFT"));

		verify(useCase).getMyCampaigns(OWNER_ID);
	}

	@Test
	void returnsOwnedCampaignById() throws Exception {
		when(useCase.getMyCampaignById(CAMPAIGN_ID, OWNER_ID))
				.thenReturn(campaign(CampaignStatus.DRAFT));

		mockMvc.perform(get("/api/campaigns/me/{campaignId}", CAMPAIGN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CAMPAIGN_ID));

		verify(useCase).getMyCampaignById(CAMPAIGN_ID, OWNER_ID);
	}

	@Test
	void passesAllPublicSearchFiltersToUseCase() throws Exception {
		when(useCase.searchCampaigns(any(CampaignSearchCriteria.class)))
				.thenReturn(List.of(campaign(CampaignStatus.RECRUITING)));

		mockMvc.perform(get("/api/campaigns/public")
				.param("name", "  curse  ")
				.param("format", "ONLINE")
				.param("characterLevel", "5")
				.param("dayOfWeek", "SATURDAY"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$[0].status").value("RECRUITING"));

		ArgumentCaptor<CampaignSearchCriteria> captor =
				ArgumentCaptor.forClass(CampaignSearchCriteria.class);
		verify(useCase).searchCampaigns(captor.capture());
		assertEquals("curse", captor.getValue().name());
		assertEquals(CampaignFormat.ONLINE, captor.getValue().format());
		assertEquals(5, captor.getValue().characterLevel());
		assertEquals(DayOfWeek.SATURDAY, captor.getValue().dayOfWeek());
	}

	@Test
	void passesEmptyCriteriaWhenPublicSearchFiltersAreMissing() throws Exception {
		when(useCase.searchCampaigns(any(CampaignSearchCriteria.class)))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/campaigns/public"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		ArgumentCaptor<CampaignSearchCriteria> captor =
				ArgumentCaptor.forClass(CampaignSearchCriteria.class);
		verify(useCase).searchCampaigns(captor.capture());
		assertNull(captor.getValue().name());
		assertNull(captor.getValue().format());
		assertNull(captor.getValue().characterLevel());
		assertNull(captor.getValue().dayOfWeek());
	}

	@Test
	void returnsPublicCampaignById() throws Exception {
		when(useCase.getPublicCampaignById(CAMPAIGN_ID))
				.thenReturn(campaign(CampaignStatus.RECRUITING));

		mockMvc.perform(get("/api/campaigns/public/{campaignId}", CAMPAIGN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$.status").value("RECRUITING"));
	}

	@Test
	void rejectsInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/campaigns")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequestJson()))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	@Test
	void rejectsInvalidUpdateRequest() throws Exception {
		mockMvc.perform(put("/api/campaigns/{campaignId}", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequestJson()))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 21})
	void rejectsPublicSearchLevelOutsideAllowedRange(int characterLevel) throws Exception {
		mockMvc.perform(get("/api/campaigns/public")
				.param("characterLevel", Integer.toString(characterLevel)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	@Test
	void mapsDomainValidationFailureToBadRequest() throws Exception {
		when(useCase.createCampaign(any(CreateCampaignCommand.class)))
				.thenThrow(new InvalidCampaignLevelRangeException());

		mockMvc.perform(post("/api/campaigns")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void mapsMissingCampaignToNotFound() throws Exception {
		when(useCase.getPublicCampaignById(CAMPAIGN_ID))
				.thenThrow(new CampaignNotFoundException(CAMPAIGN_ID));

		mockMvc.perform(get("/api/campaigns/public/{campaignId}", CAMPAIGN_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void mapsMissingCurrentUserToNotFound() throws Exception {
		when(useCase.createCampaign(any(CreateCampaignCommand.class)))
				.thenThrow(new UserNotFoundException(OWNER_ID));

		mockMvc.perform(post("/api/campaigns")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void mapsForeignCampaignAccessToForbidden() throws Exception {
		when(useCase.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
				.thenThrow(new CampaignAccessDeniedException());

		mockMvc.perform(put("/api/campaigns/{campaignId}", CAMPAIGN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void mapsInvalidStatusTransitionToConflict() throws Exception {
		when(useCase.startCampaign(CAMPAIGN_ID, OWNER_ID))
				.thenThrow(new InvalidCampaignStatusTransitionException(
						CampaignStatus.DRAFT,
						CampaignStatus.IN_PROGRESS));

		mockMvc.perform(post("/api/campaigns/{campaignId}/start", CAMPAIGN_ID))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	private void stubStatusAction(StatusAction action) {
		Campaign result = campaign(action.resultStatus);
		switch (action) {
			case PUBLISH -> when(useCase.publishCampaign(CAMPAIGN_ID, OWNER_ID)).thenReturn(result);
			case START -> when(useCase.startCampaign(CAMPAIGN_ID, OWNER_ID)).thenReturn(result);
			case COMPLETE -> when(useCase.completeCampaign(CAMPAIGN_ID, OWNER_ID)).thenReturn(result);
			case CANCEL -> when(useCase.cancelCampaign(CAMPAIGN_ID, OWNER_ID)).thenReturn(result);
		}
	}

	private void verifyStatusAction(StatusAction action) {
		switch (action) {
			case PUBLISH -> verify(useCase).publishCampaign(CAMPAIGN_ID, OWNER_ID);
			case START -> verify(useCase).startCampaign(CAMPAIGN_ID, OWNER_ID);
			case COMPLETE -> verify(useCase).completeCampaign(CAMPAIGN_ID, OWNER_ID);
			case CANCEL -> verify(useCase).cancelCampaign(CAMPAIGN_ID, OWNER_ID);
		}
	}

	private Campaign campaign(CampaignStatus status) {
		return new Campaign(
				CAMPAIGN_ID,
				OWNER_ID,
				"Curse of Strahd",
				"Gothic horror campaign",
				status,
				CampaignFormat.ONLINE,
				6,
				3,
				8,
				new CampaignSchedule(
						DayOfWeek.SATURDAY,
						LocalTime.of(19, 30),
						ZoneId.of("Europe/Moscow")),
				"Reliable weekly attendance",
				Instant.parse("2026-11-01T10:00:00Z"),
				Instant.parse("2026-11-02T10:00:00Z"));
	}

	private String validRequestJson() {
		return """
				{
				  "name": "Curse of Strahd",
				  "description": "Gothic horror campaign",
				  "format": "ONLINE",
				  "maxPlayerCount": 6,
				  "minLevel": 3,
				  "maxLevel": 8,
				  "schedule": {
				    "dayOfWeek": "SATURDAY",
				    "startTime": "19:30:00",
				    "timeZone": "Europe/Moscow"
				  },
				  "requirements": "Reliable weekly attendance"
				}
				""";
	}

	private String invalidRequestJson() {
		return """
				{
				  "name": " ",
				  "description": "Gothic horror campaign",
				  "format": "ONLINE",
				  "maxPlayerCount": 0,
				  "minLevel": 0,
				  "maxLevel": 21,
				  "schedule": null,
				  "requirements": "Reliable weekly attendance"
				}
				""";
	}

	private enum StatusAction {
		PUBLISH("publish", CampaignStatus.RECRUITING),
		START("start", CampaignStatus.IN_PROGRESS),
		COMPLETE("complete", CampaignStatus.COMPLETED),
		CANCEL("cancel", CampaignStatus.CANCELLED);

		private final String path;
		private final CampaignStatus resultStatus;

		StatusAction(String path, CampaignStatus resultStatus) {
			this.path = path;
			this.resultStatus = resultStatus;
		}
	}
}
