package com.dndcrowler.dnd_backend.adapter.in.web.campaignMember;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CampaignMemberUseCase;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignMemberNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignRosterChangeNotAllowedException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@ExtendWith(MockitoExtension.class)
class CampaignMemberControllerTest {
	private static final Long USER_ID = 42L;
	private static final Long CAMPAIGN_ID = 100L;
	private static final Long MEMBER_ID = 200L;
	private static final Long CHARACTER_ID = 300L;
	private static final Instant JOINED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	CampaignMemberUseCase useCase;

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

		var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		var proxyFactory = new ProxyFactory(new CampaignMemberController(useCase));
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice(new MethodValidationInterceptor(
				(jakarta.validation.Validator) validator));
		var controller = (CampaignMemberController) proxyFactory.getProxy();

		mockMvc = MockMvcBuilders
				.standaloneSetup(controller)
				.setControllerAdvice(new CampaignMemberExceptionHandler())
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void returnsCampaignMembersToGameMaster() throws Exception {
		when(useCase.getCampaignMembers(CAMPAIGN_ID, USER_ID))
				.thenReturn(List.of(member()));

		mockMvc.perform(get("/api/campaigns/{campaignId}/members", CAMPAIGN_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(MEMBER_ID))
				.andExpect(jsonPath("$[0].campaignId").value(CAMPAIGN_ID))
				.andExpect(jsonPath("$[0].userId").value(USER_ID))
				.andExpect(jsonPath("$[0].characterId").value(CHARACTER_ID))
				.andExpect(jsonPath("$[0].joinedAt").value(JOINED_AT.toString()));

		verify(useCase).getCampaignMembers(CAMPAIGN_ID, USER_ID);
	}

	@Test
	void returnsMembershipsForAuthenticatedUser() throws Exception {
		when(useCase.getMyMemberships(USER_ID)).thenReturn(List.of(member()));

		mockMvc.perform(get("/api/campaign-members/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(MEMBER_ID));

		verify(useCase).getMyMemberships(USER_ID);
	}

	@Test
	void gameMasterRemovesMember() throws Exception {
		mockMvc.perform(delete(
				"/api/campaigns/{campaignId}/members/{memberId}",
				CAMPAIGN_ID,
				MEMBER_ID))
				.andExpect(status().isNoContent());

		verify(useCase).removeMember(CAMPAIGN_ID, MEMBER_ID, USER_ID);
	}

	@Test
	void authenticatedUserLeavesCampaign() throws Exception {
		mockMvc.perform(delete(
				"/api/campaigns/{campaignId}/membership",
				CAMPAIGN_ID))
				.andExpect(status().isNoContent());

		verify(useCase).leaveCampaign(CAMPAIGN_ID, USER_ID);
	}

	@Test
	void mapsMissingCampaignTo404() throws Exception {
		when(useCase.getCampaignMembers(CAMPAIGN_ID, USER_ID))
				.thenThrow(new CampaignNotFoundException(CAMPAIGN_ID));

		mockMvc.perform(get("/api/campaigns/{campaignId}/members", CAMPAIGN_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail")
						.value("Campaign not found: id=100"));
	}

	@Test
	void mapsMissingMembershipTo404() throws Exception {
		when(useCase.getMyMemberships(USER_ID))
				.thenThrow(new CampaignMemberNotFoundException(CAMPAIGN_ID, USER_ID));

		mockMvc.perform(get("/api/campaign-members/me"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mapsAccessDeniedTo403() throws Exception {
		when(useCase.getCampaignMembers(CAMPAIGN_ID, USER_ID))
				.thenThrow(new CampaignAccessDeniedException());

		mockMvc.perform(get("/api/campaigns/{campaignId}/members", CAMPAIGN_ID))
				.andExpect(status().isForbidden());
	}

	@Test
	void mapsRosterConflictTo409() throws Exception {
		org.mockito.Mockito.doThrow(new CampaignRosterChangeNotAllowedException(
				CAMPAIGN_ID,
				CampaignStatus.IN_PROGRESS))
				.when(useCase)
				.removeMember(CAMPAIGN_ID, MEMBER_ID, USER_ID);

		mockMvc.perform(delete(
				"/api/campaigns/{campaignId}/members/{memberId}",
				CAMPAIGN_ID,
				MEMBER_ID))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectsNonPositivePathId() throws Exception {
		mockMvc.perform(get("/api/campaigns/{campaignId}/members", 0L))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(useCase);
	}

	private CampaignMember member() {
		return new CampaignMember(
				MEMBER_ID,
				CAMPAIGN_ID,
				USER_ID,
				CHARACTER_ID,
				JOINED_AT);
	}
}
