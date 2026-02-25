package com.example.demo.contributor;

import org.springframework.stereotype.Component;

import com.example.demo.contributor.dto.ContributorDto;

@Component
public class ContributorMapper {

    public ContributorDto toDto(Contributor contributor) {
        return new ContributorDto(
                contributor.getContributorId(),
                contributor.getPromotedAt(),
                contributor.getDemotedAt()
        );
    }
}
