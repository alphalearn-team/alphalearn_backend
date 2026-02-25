package com.example.demo.contributor;

import java.util.List;

import com.example.demo.contributor.dto.response.ContributorDto;
import org.springframework.stereotype.Service;

@Service
public class ContributorService {

    private final ContributorRepository contributorRepository;
    private final ContributorMapper contributorMapper;

    public ContributorService(ContributorRepository contributorRepository, ContributorMapper contributorMapper) {
        this.contributorRepository = contributorRepository;
        this.contributorMapper = contributorMapper;
    }

    public List<ContributorDto> getAllContributors() {
        return contributorRepository.findAll().stream()
                .map(contributorMapper::toDto)
                .toList();
    }
}
