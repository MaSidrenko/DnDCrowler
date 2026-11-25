package com.dndcrowler.dnd_backend.adapter.in.web.campaign;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Request.CreateCampaignRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Request.UpdateCampaignRequest;
import com.dndcrowler.dnd_backend.adapter.in.web.campaign.DTO.Response.CampaignResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CampaignUseCase;
import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {
	private final CampaignUseCase useCase;

	public CampaignController(CampaignUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CampaignResponse create(@Valid @RequestBody CreateCampaignRequest request,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.createCampaign(request.toCommand(principal.userId())));
	}

	@PutMapping("/{campaignId}")
	public CampaignResponse update(@PathVariable Long campaignId,
			@Valid @RequestBody UpdateCampaignRequest request,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.updateCampaign(campaignId,
						request.toCommand(principal.userId())));
	}

	@PostMapping("/{campaignId}/publish")
	public CampaignResponse publish(@PathVariable Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.publishCampaign(campaignId, principal.userId()));
	}

	@PostMapping("/{campaignId}/start")
	public CampaignResponse start(
			@PathVariable Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.startCampaign(campaignId, principal.userId()));
	}

	@PostMapping("/{campaignId}/complete")
	public CampaignResponse complete(@PathVariable Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.completeCampaign(campaignId, principal.userId()));
	}

	@PostMapping("/{campaignId}/cancel")
	public CampaignResponse cancel(@PathVariable Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.cancelCampaign(campaignId, principal.userId()));
	}

	@GetMapping("/me")
	public List<CampaignResponse> getMyCampaign(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase.getMyCampaigns(principal.userId())
				.stream()
				.map(CampaignResponse::from)
				.toList();
	}

	@GetMapping("/me/{campaignId}")
	public CampaignResponse getMyById(@PathVariable Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return CampaignResponse.from(
				useCase.getMyCampaignById(campaignId, principal.userId()));
	}

	@GetMapping("/public")
	public List<CampaignResponse> searchPublicCampaign(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) CampaignFormat format,
			@RequestParam(required = false) @Min(1) @Max(20) Integer characterLevel,
			@RequestParam(required = false) DayOfWeek dayOfWeek) {
		CampaignSearchCriteria criteria = new CampaignSearchCriteria(name, format, characterLevel, dayOfWeek);

		return useCase.searchCampaigns(criteria)
				.stream()
				.map(CampaignResponse::from)
				.toList();
	}

	@GetMapping("/public/{campaignId}")
	public CampaignResponse getPublicCampaignById(
			@PathVariable Long campaignId) {
		return CampaignResponse.from(
				useCase.getPublicCampaignById(campaignId));
	}

}
