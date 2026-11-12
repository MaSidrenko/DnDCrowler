package com.dndcrowler.dnd_backend.adapter.in.web.auth;

import org.springframework.mock.web.MockHttpSession;

/**
 * CsrfData
 */
public record CsrfData(
	MockHttpSession session,
	String headerName,
	String token
) {

}
