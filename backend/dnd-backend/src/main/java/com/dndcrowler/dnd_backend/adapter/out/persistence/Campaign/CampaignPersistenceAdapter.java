package com.dndcrowler.dnd_backend.adapter.out.persistence.Campaign;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.dndcrowler.dnd_backend.application.port.in.query.CampaignSearchCriteria;
import com.dndcrowler.dnd_backend.application.port.out.Campaign.CampaignRepositoryPort;
import com.dndcrowler.dnd_backend.domain.model.Campaign.Campaign;
import com.dndcrowler.dnd_backend.domain.model.Campaign.CampaignStatus;

@Repository
public class CampaignPersistenceAdapter implements CampaignRepositoryPort {
	private static final Set<CampaignStatus> PUBLIC_STATUSES = Set.of(
			CampaignStatus.RECRUITING,
			CampaignStatus.FULL,
			CampaignStatus.IN_PROGRESS,
			CampaignStatus.COMPLETED);
	private final SpringDataCampaignRepository repository;

	public CampaignPersistenceAdapter(SpringDataCampaignRepository repository) {
		this.repository = repository;
	}

	@Override
	public Campaign save(Campaign campaign) {
		return repository.saveAndFlush(new CampaignJpaEntity(campaign)).toDomain();
	}

	@Override
	public Optional<Campaign> findById(Long campaignId) {
		return repository.findById(campaignId).map(CampaignJpaEntity::toDomain);
	}

	@Override
	public Optional<Campaign> findPubliclyVisibleById(Long campaignId) {
		return repository.findByIdAndStatusIn(campaignId, PUBLIC_STATUSES)
				.map(CampaignJpaEntity::toDomain);
	}

	@Override
	public List<Campaign> findByGameMasterId(Long gameMasterId) {
		return repository.findByGameMasterId(gameMasterId).stream()
				.map(CampaignJpaEntity::toDomain).toList();
	}

	@Override
	public List<Campaign> searchRecruiting(CampaignSearchCriteria criteria) {
		Specification<CampaignJpaEntity> specification = (root, query, cb) -> cb.equal(
				root.get("status"),
				CampaignStatus.RECRUITING);

		if (criteria.name() != null) {
			String namePattern = "%"
					+ criteria.name().toLowerCase(Locale.ROOT)
					+ "%";

			specification = specification.and(
					(root, query, cb) -> cb.like(
							cb.lower(root.get("name")),
							namePattern));
		}

		if (criteria.format() != null) {
			specification = specification.and(
					(root, query, cb) -> cb.equal(
							root.get("format"),
							criteria.format()));
		}

		if (criteria.characterLevel() != null) {
			specification = specification.and(
					(root, query, cb) -> cb.and(
							cb.lessThanOrEqualTo(root.get("minLevel"), criteria.characterLevel()),
							cb.greaterThanOrEqualTo(root.get("maxLevel"), criteria.characterLevel())));
		}

		if (criteria.dayOfWeek() != null) {
			specification = specification.and(
					(root, query, cb) -> cb.equal(
							root.get("schedule").get("dayOfWeek"),
							criteria.dayOfWeek()));
		}

		return repository.findAll(specification)
				.stream()
				.map(CampaignJpaEntity::toDomain)
				.toList();
	}
}
