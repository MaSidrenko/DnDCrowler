package com.dndcrowler.dnd_backend.adapter.in.web.campaign;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.dndcrowler.dnd_backend.adapter.in.web.auth.CsrfData;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignSecurityIntegrationTest {
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	private final MockMvc mockMvc;

	@Autowired
	CampaignSecurityIntegrationTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void allowsAnonymousUserToSearchPublicCampaigns() throws Exception {
		mockMvc.perform(get("/api/campaigns/public"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void allowsAnonymousUserToRequestPublicCampaignById() throws Exception {
		mockMvc.perform(get("/api/campaigns/public/{campaignId}", 999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void rejectsAnonymousAccessToOwnedCampaigns() throws Exception {
		mockMvc.perform(get("/api/campaigns/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnonymousCampaignCreationEvenWithValidCsrf() throws Exception {
		CsrfData csrf = obtainCsrf();

		mockMvc.perform(post("/api/campaigns")
				.session(csrf.session())
				.header(csrf.headerName(), csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAuthenticatedCampaignCreationWithoutCsrf() throws Exception {
		mockMvc.perform(post("/api/campaigns")
				.session(authenticatedSession())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequestJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void allowsAuthenticatedUserToRequestOwnedCampaigns() throws Exception {
		mockMvc.perform(get("/api/campaigns/me")
				.session(authenticatedSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	private CsrfData obtainCsrf() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();

		CsrfToken token = (CsrfToken) result.getRequest()
				.getAttribute(CsrfToken.class.getName());
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		assertNotNull(token);
		assertNotNull(session);

		return new CsrfData(session, token.getHeaderName(), token.getToken());
	}

	private MockHttpSession authenticatedSession() {
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
				42L,
				"alice",
				"alice@example.com",
				UserRole.USER);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				context);
		return session;
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
}
