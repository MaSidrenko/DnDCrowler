package com.dndcrowler.dnd_backend.application.service.CampaignApplication;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.dndcrowler.dnd_backend.application.port.in.CampaignApplicationUseCase;
import com.dndcrowler.dnd_backend.application.port.in.command.CampaignApplication.SubmitCampaignApplicationCommand;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignApplication.CampaignApplicationRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.CampaignMember.CampaignMemberRepositoryPort;
import com.dndcrowler.dnd_backend.application.port.out.Character.CharacterRepositoryPort;
import com.dndcrowler.dnd_backend.application.service.exception.CampaignNotFoundException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterAccessDeniedException;
import com.dndcrowler.dnd_backend.application.service.exception.CharacterNotFoundException;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;
import com.dndcrowler.dnd_backend.domain.model.CampaignApplication.CampaignApplication;
import com.dndcrowler.dnd_backend.domain.model.CampaignMember.CampaignMember;
import com.dndcrowler.dnd_backend.domain.model.Character.Character;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignApplicationService implements CampaignApplicationUseCase {
	private final CampaignApplicationRepositoryPort applicationRepository;
	private final CampaignMemberRepositoryPort memberRepository;
	private final CampaignRepositoryPort campaignRepository;
	private final CharacterRepositoryPort characterRepository;

	public CampaignApplicationService(
			CampaignApplicationRepositoryPort applicationRepository,
			CampaignMemberRepositoryPort memberRepository,
			CampaignRepositoryPort campaignRepository,
			CharacterRepositoryPort characterRepository) {
		this.applicationRepository = applicationRepository;
		this.memberRepository = memberRepository;
		this.campaignRepository = campaignRepository;
		this.characterRepository = characterRepository;
	}

	@Override
	@Transactional
	public CampaignApplication submit(SubmitCampaignApplicationCommand command) {
		Campaign campaign = campaignRepository
				.findByIdForUpdate(command.campaignId())
				.orElseThrow(() -> new CampaignNotFoundException(command.campaignId()));

		if (campaign.status() != CampaignStatus.RECRUITING) {
			throw new IllegalArgumentException();
		}

		if (campaign.gameMasterId().equals(command.currentUserId())) {
			throw new IllegalArgumentException();
		}
		Character character = characterRepository
				.findById(command.characterId())
				.orElseThrow(() -> new CharacterNotFoundException(command.characterId()));

		if (!character.ownerID().equals(command.currentUserId())) {
			throw new CharacterAccessDeniedException();
		}

		if (character.level() < campaign.minLevel()
				|| character.level() > campaign.maxLevel()) {
			throw new IllegalArgumentException();
		}

		if (applicationRepository.existsActiveByCampaignIdAndApplicantId(
				campaign.id(),
				command.currentUserId())) {
			throw new IllegalArgumentException();
		}

		if (memberRepository.existsByCampaignIdAndUserId(
				campaign.id(),
				command.currentUserId())) {
			throw new IllegalArgumentException();
		}

		CampaignApplication newApplication = CampaignApplication
				.create(campaign.id(),
						command.currentUserId(),
						character.id(),
						command.message());

		return applicationRepository.save(newApplication);

	}

	@Override
	@Transactional
	public CampaignApplication accept(Long applicationId, Long currentUserId) {
		CampaignApplication application = findApplicationForUpdate(applicationId);

		Campaign campaign = campaignRepository
				.findByIdForUpdate(application.campaignId())
				.orElseThrow(() -> new IllegalArgumentException());

		ensureGameMaster(campaign, currentUserId);
		ensureRecruiting(campaign);

		boolean alreadyMember = memberRepository.existsByCampaignIdAndUserId(
				campaign.id(), application.applicantId());

		if (alreadyMember) {
			throw new IllegalArgumentException();
		}

		long currentMemberCount = memberRepository.countByCampaignId(campaign.id());

		if (currentMemberCount >= campaign.maxPlayerCount()) {
			throw new IllegalArgumentException();
		}

		Character character = characterRepository
				.findById(application.characterId())
				.orElseThrow(() -> new CharacterNotFoundException(application.characterId()));

		validateCharacter(character, campaign, application.applicantId());

		CampaignApplication acceptedApplication = application.accept();

		CampaignMember member = CampaignMember.create(
				campaign.id(), application.applicantId(), character.id());

		memberRepository.save(member);

		CampaignApplication savedApplication = applicationRepository.save(acceptedApplication);

		if (currentMemberCount + 1 >= campaign.maxPlayerCount()) {
			campaignRepository.save(campaign.markFull());
		}

		return savedApplication;
	}

	@Override
	@Transactional
	public CampaignApplication reject(Long applicationId, Long currentUserId) {
		CampaignApplication application = findApplicationForUpdate(applicationId);

		Campaign campaign = campaignRepository
				.findById(application.campaignId())
				.orElseThrow(() -> new CampaignNotFoundException(application.campaignId()));

		ensureGameMaster(campaign, currentUserId);

		CampaignApplication rejectedApplication = application.reject();

		return applicationRepository.save(rejectedApplication);
	}

	@Override
	@Transactional
	public CampaignApplication withdraw(Long applicationId, Long currentUserId) {
		CampaignApplication application = findApplicationForUpdate(applicationId);

		if (!Objects.equals(
				application.applicantId(), currentUserId)) {
			throw new IllegalArgumentException();
		}

		CampaignApplication withdrawApplication = application.withdraw();

		return applicationRepository.save(
				withdrawApplication);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignApplication> getMyApplications(Long currentUserId) {
		return applicationRepository.findByApplicantId(currentUserId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CampaignApplication> getCampaignApplications(Long campaignId, Long currentUserId) {
		Campaign campaign = campaignRepository
				.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException(campaignId));

		ensureGameMaster(campaign, currentUserId);

		return applicationRepository.findByCampaignId(campaignId);
	}

	private CampaignApplication findApplicationForUpdate(
			Long applicationId) {
		return applicationRepository
				.findByIdForUpdate(applicationId)
				.orElseThrow(() -> new IllegalArgumentException());
	}

	private void ensureRecruiting(Campaign campaign) {
		if (campaign.status() == CampaignStatus.FULL) {
			throw new IllegalArgumentException();
		}

		if (campaign.status() != CampaignStatus.RECRUITING) {
			throw new IllegalArgumentException();
		}
	}

	private void ensureGameMaster(
			Campaign campaign,
			Long currentUserId) {
		if (!Objects.equals(campaign.gameMasterId(), currentUserId)) {
			throw new IllegalArgumentException();
		}
	}

	private void validateCharacter(
			Character character,
			Campaign campaign,
			Long expectedOwnerId) {
		if (!Objects.equals(character.ownerID(), expectedOwnerId)) {
			throw new CharacterAccessDeniedException();
		}

		boolean levelTooLow = character.level() < campaign.minLevel();

		boolean levelTooHigh = character.level() > campaign.maxLevel();

		if (levelTooLow || levelTooHigh) {
			throw new IllegalArgumentException();
		}
	}
}
