package com.example.demo.contributor;

import java.util.List;

import com.example.demo.contributor.dto.ContributorDto;
import org.springframework.stereotype.Service;

@Service
public class ContributorQueryService {

    private final ContributorRepository contributorRepository;
    private final ContributorMapper contributorMapper;

    public ContributorQueryService(ContributorRepository contributorRepository, ContributorMapper contributorMapper) {
        this.contributorRepository = contributorRepository;
        this.contributorMapper = contributorMapper;
    }

    public List<ContributorDto> getAllContributors() {
        return contributorRepository.findAll().stream()
                .map(contributorMapper::toDto)
                .toList();
    }
}
