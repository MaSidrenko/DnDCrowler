package com.dndcrowler.dnd_backend.application.service.Campaign;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.CampaignUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.CreateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.command.Campaign.UpdateCampaignCommand;
import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.User.UserRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.UserNotFoundException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;

import jakarta.transaction.Transactional;

@Service
public class CampaignService implements CampaignUseCase {
	private final UserRepositoryPort userRepositoryPort;
	private final CampaignRepositoryPort campaignRepositoryPort;

	public CampaignService(UserRepositoryPort userRepositoryPort,
			CampaignRepositoryPort campaignRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.campaignRepositoryPort = campaignRepositoryPort;
	}

	@Override
	@Transactional
	public Campaign createCampaign(CreateCampaignCommand command) {
		userRepositoryPort.findById(command.currentUserId())
				.orElseThrow(() -> new UserNotFoundException(command.currentUserId()));

		Campaign newCampaign = Campaign.create(
				command.currentUserId(),
				command.name(),
				command.description(),
				command.format(),
				command.maxPlayerCount(),
				command.minLevel(),
				command.maxLevel(),
				command.schedule(),
				command.requirements());

		return campaignRepositoryPort.save(newCampaign);
	}

	@Override
	@Transactional
	public Campaign updateCampaign(Long campaignId, UpdateCampaignCommand command) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));

		if (!existingCampaign.gameMasterId().equals(command.currentUserId())) {
			throw new CampaignAccessDeniedException();
		}

		Campaign newDataCampaign = existingCampaign.update(command.name(),
				command.description(), command.format(), command.maxPlayerCount(),
				command.minLevel(), command.maxLevel(), command.schedule(),
				command.requirements());

		return campaignRepositoryPort.save(newDataCampaign);
	}

	@Override
	@Transactional 
	public Campaign publishCampaign(Long campaignId, Long currentUserId) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!existingCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}
		Campaign publishedCampaign = existingCampaign.publish();

		return campaignRepositoryPort.save(publishedCampaign);
	}

	@Override
	@Transactional 
	public Campaign startCampaign(Long campaignId, Long currentUserId) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!existingCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}
		Campaign startedCampaign = existingCampaign.start();

		return campaignRepositoryPort.save(startedCampaign);
	}

	@Override
	@Transactional 
	public Campaign completeCampaign(Long campaignId, Long currentUserId) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!existingCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}
		Campaign completedCampaign = existingCampaign.complete();

		return campaignRepositoryPort.save(completedCampaign);
	}

	@Override
	@Transactional 
	public Campaign cancelCampaign(Long campaignId, Long currentUserId) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!existingCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}
		Campaign cancelledCampaign = existingCampaign.cancel();

		return campaignRepositoryPort.save(cancelledCampaign);
	}

	@Override
	public Campaign getPublicCampaignById(Long campaignId) {
		return campaignRepositoryPort.findPubliclyVisibleById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
	}

	@Override
	public Campaign getMyCampaignById(Long campaignId, Long currentUserId) {
		Campaign existingCampaign = campaignRepositoryPort.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));
		if (!existingCampaign.gameMasterId().equals(currentUserId)) {
			throw new CampaignAccessDeniedException();
		}

		return existingCampaign;
	}

	@Override
	public List<Campaign> getMyCampaigns(Long currentUserId) {
		return campaignRepositoryPort.findByGameMasterId(currentUserId);
	}

	@Override
	public List<Campaign> searchCampaigns(CampaignSearchCriteria criteria) {
		return campaignRepositoryPort.searchRecruiting(criteria);
	}

}
