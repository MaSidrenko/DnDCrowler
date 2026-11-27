package com.dndcrowler.dnd_backend.adapter.in.web.campaignMember;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dndcrowler.dnd_backend.adapter.in.web.campaignMember.DTO.Response.CampaignMemberResponse;
import com.dndcrowler.dnd_backend.adapter.in.web.security.AuthenticatedUserPrincipal;
import com.dndcrowler.dnd_backend.application.port.in.CampaignMemberUseCase;

import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api")
public class CampaignMemberController {
	private final CampaignMemberUseCase useCase;

	public CampaignMemberController(CampaignMemberUseCase useCase) {
		this.useCase = useCase;
	}

	@GetMapping("/campaigns/{campaignId}/members")
	public List<CampaignMemberResponse> getCampaignMembers(
			@PathVariable @Positive Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase
				.getCampaignMembers(campaignId, principal.userId())
				.stream()
				.map(CampaignMemberResponse::from)
				.toList();
	}

	@GetMapping("/campaign-members/me")
	public List<CampaignMemberResponse> getMemberships(
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return useCase
				.getMyMemberships(principal.userId())
				.stream()
				.map(CampaignMemberResponse::from)
				.toList();
	}

	@DeleteMapping("/campaigns/{campaignId}/members/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(
			@PathVariable @Positive Long campaignId,
			@PathVariable @Positive Long memberId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		useCase.removeMember(campaignId, memberId, principal.userId());
	}

	@DeleteMapping("/campaigns/{campaignId}/membership")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leaveCampaign(
			@PathVariable @Positive Long campaignId,
			@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		useCase.leaveCampaign(campaignId, principal.userId());
	}
}
