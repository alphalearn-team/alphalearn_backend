package com.example.demo.contributor;

import org.springframework.stereotype.Component;

import com.example.demo.contributor.dto.ContributorPublicDto;

@Component
public class ContributorMapper {

    public ContributorPublicDto toPublicDto(Contributor contributor) {
        return new ContributorPublicDto(
                contributor.getLearner() == null ? null : contributor.getLearner().getPublicId(),
                contributor.getLearner() == null ? null : contributor.getLearner().getUsername(),
                contributor.getPromotedAt(),
                contributor.getDemotedAt()
        );
    }
}
