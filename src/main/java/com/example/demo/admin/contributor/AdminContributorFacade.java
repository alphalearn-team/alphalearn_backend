package com.example.demo.admin.contributor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.demo.contributor.Contributor;
import com.example.demo.contributor.ContributorMapper;
import com.example.demo.contributor.ContributorQueryService;
import com.example.demo.contributor.ContributorRepository;
import com.example.demo.contributor.dto.ContributorPublicDto;
import com.example.demo.contributor.dto.DemoteContributorsRequest;
import com.example.demo.contributor.dto.PromoteContributorsRequest;
import com.example.demo.learner.Learner;
import com.example.demo.learner.LearnerRepository;
import com.example.demo.lesson.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminContributorFacade {

    private final ContributorRepository contributorRepository;
    private final LearnerRepository learnerRepository;
    private final LessonRepository lessonRepository;
    private final ContributorMapper contributorMapper;
    private final ContributorQueryService contributorQueryService;

    public AdminContributorFacade(
            ContributorRepository contributorRepository,
            LearnerRepository learnerRepository,
            LessonRepository lessonRepository,
            ContributorMapper contributorMapper,
            ContributorQueryService contributorQueryService
    ) {
        this.contributorRepository = contributorRepository;
        this.learnerRepository = learnerRepository;
        this.lessonRepository = lessonRepository;
        this.contributorMapper = contributorMapper;
        this.contributorQueryService = contributorQueryService;
    }

    @Transactional(readOnly = true)
    public List<ContributorPublicDto> getAllContributors() {
        return contributorQueryService.getAllPublicContributors();
    }

    @Transactional
    public List<ContributorPublicDto> promoteLearners(PromoteContributorsRequest request) {
        if (request == null || request.learnerPublicIds() == null || request.learnerPublicIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "learnerPublicIds are required");
        }

        List<Contributor> created = new ArrayList<>();
        for (UUID learnerPublicId : request.learnerPublicIds()) {
            if (learnerPublicId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "learnerPublicIds cannot contain null");
            }

            Learner learner = learnerRepository.findByPublicId(learnerPublicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learner not found: " + learnerPublicId));
            UUID learnerId = learner.getId();

            if (learner.getId() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Learner id is null for publicId: " + learnerPublicId);
            }

            Contributor contributor = contributorRepository.findById(learnerId).orElse(null);
            if (contributor != null) {
                if (contributor.isCurrentContributor()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Already a contributor: " + learnerPublicId);
                }

                contributor.setPromotedAt(OffsetDateTime.now());
                contributor.setDemotedAt(null);
                created.add(contributorRepository.save(contributor));
                continue;
            }

            Contributor newContributor = new Contributor(
                    learnerId,
                    null,
                    OffsetDateTime.now(),
                    null
            );
            created.add(contributorRepository.save(newContributor));
        }

        return created.stream()
                .map(contributorMapper::toPublicDto)
                .toList();
    }

    @Transactional
    public void demoteContributors(DemoteContributorsRequest request) {
        if (request == null || request.contributorPublicIds() == null || request.contributorPublicIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contributorPublicIds are required");
        }

        for (UUID contributorPublicId : request.contributorPublicIds()) {
            if (contributorPublicId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contributorPublicIds cannot contain null");
            }
            if (!contributorRepository.existsByLearner_PublicId(contributorPublicId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found: " + contributorPublicId);
            }
        }

        for (UUID contributorPublicId : request.contributorPublicIds()) {
            Contributor contributor = contributorRepository.findByLearner_PublicId(contributorPublicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contributor not found " + contributorPublicId));
            UUID contributorId = contributor.getContributorId();
            contributor.setDemotedAt(OffsetDateTime.now());
            lessonRepository.unpublishByContributorId(contributorId);
        }
    }
}
