package com.dndcrowler.dnd_backend.adapter.in.web.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
	@Bean
	OpenApiCustomizer logoutOpenApiCustomizer() {
		return openApi -> openApi.path(
				"/api/auth/logout",
				new PathItem().post(
						new Operation()
								.operationId("logout")
								.summary("Logout")
								.description("Invalidates the current HTTP session")
								.addTagsItem("Authentication")
								.addParametersItem(
										new Parameter()
												.name("X-CSRF-TOKEN")
												.in("header")
												.required(true)
												.description("Token received from GET /api/auth/csrf")
												.schema(new StringSchema()))
								.responses(
										new ApiResponses()
												.addApiResponse(
														"204",
														new ApiResponse().description("Logout successful"))
												.addApiResponse(
														"403",
														new ApiResponse().description(
																"CSRF token is missing or invalid")))));
	}
}
