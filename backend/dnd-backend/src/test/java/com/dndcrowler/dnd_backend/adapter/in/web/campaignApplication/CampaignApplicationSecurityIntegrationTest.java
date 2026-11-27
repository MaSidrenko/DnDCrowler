package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import com.dndcrowler.dnd_backend.adapter.in.web.auth.CsrfData;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.domain.model.User.UserRole;
import com.dndcrowler.dnd_backend.support.CampaignApplicationPostgresTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignApplicationSecurityIntegrationTest
		extends CampaignApplicationPostgresTestSupport {

	private final MockMvc mockMvc;

	@Autowired
	CampaignApplicationSecurityIntegrationTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void rejectsAnonymousApplicationListing() throws Exception {
		mockMvc.perform(get("/api/campaign-applications/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnonymousSubmissionEvenWithCsrf() throws Exception {
		CsrfData csrf = obtainCsrf();

		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", 999L)
				.session(csrf.session())
				.header(csrf.headerName(), csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"characterId\":1}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAuthenticatedSubmissionWithoutCsrf() throws Exception {
		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", 999L)
				.session(authenticatedSession())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"characterId\":1}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void allowsAuthenticatedApplicationListing() throws Exception {
		mockMvc.perform(get("/api/campaign-applications/me")
				.session(authenticatedSession()))
				.andExpect(status().isOk());
	}

	@Test
	void authenticatedSubmissionWithCsrfReachesApplicationLayer() throws Exception {
		MockHttpSession session = authenticatedSession();
		CsrfToken token = csrfTokenFor(session);

		mockMvc.perform(post("/api/campaigns/{campaignId}/applications", 999L)
				.session(session)
				.header(token.getHeaderName(), token.getToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"characterId\":1}"))
				.andExpect(status().isNotFound());
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

	private CsrfToken csrfTokenFor(MockHttpSession session) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
				.andExpect(status().isOk())
				.andReturn();
		CsrfToken token = (CsrfToken) result.getRequest()
				.getAttribute(CsrfToken.class.getName());
		assertNotNull(token);
		return token;
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
}
