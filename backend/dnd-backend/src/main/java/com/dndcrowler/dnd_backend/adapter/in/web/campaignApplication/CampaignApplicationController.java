package com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication.DTO.Request.SubmitCampaignApplicationRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.campaignApplication.DTO.Response.CampaignApplicationResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CampaignApplicationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api")
public class CampaignApplicationController {

	private final CampaignApplicationUseCase useCase;

	public CampaignApplicationController(
			CampaignApplicationUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping("/campaigns/{campaignId}/applications")
	@ResponseStatus(HttpStatus.CREATED)
	public CampaignApplicationResponse submit(
			@PathVariable @Positive Long campaignId,

			@Valid @RequestBody SubmitCampaignApplicationRequest request,

			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		SubmitCampaignApplicationCommand command = request.toCommand(principal.userId(), campaignId);

		CampaignApplication application = useCase.submit(command);

		return CampaignApplicationResponse.from(application);
	}

	@GetMapping("/campaign-applications/me")
	public List<CampaignApplicationResponse> getMyApplications(
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase.getMyApplications(principal.userId())
				.stream()
				.map(CampaignApplicationResponse::from)
				.toList();
	}

	@GetMapping("/campaigns/{campaignId}/applications")
	public List<CampaignApplicationResponse> getCampaignApplications(
			@PathVariable @Positive Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase.getCampaignApplications(
				campaignId, principal.userId()).stream()
				.map(CampaignApplicationResponse::from)
				.toList();
	}

	@PostMapping("/campaign-applications/{applicationId}/accept")
	public CampaignApplicationResponse accept(
			@PathVariable @Positive Long applicationId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		CampaignApplication application = useCase.accept(applicationId,
				principal.userId());

		return CampaignApplicationResponse.from(application);
	}

	@PostMapping("/campaign-applications/{applicationId}/reject")
	public CampaignApplicationResponse reject(
			@PathVariable @Positive Long applicationId,

			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		CampaignApplication application = useCase.reject(applicationId, principal.userId());

		return CampaignApplicationResponse.from(application);
	}

	@PostMapping("/campaign-applications/{applicationId}/withdraw")
	public CampaignApplicationResponse withdraw(
			@PathVariable @Positive Long applicationId,

			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		CampaignApplication application = useCase.withdraw(applicationId, principal.userId());

		return CampaignApplicationResponse.from(application);
	}
}
