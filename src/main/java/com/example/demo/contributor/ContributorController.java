package com.example.demo.contributor;


import java.util.List;

import com.example.demo.contributor.dto.ContributorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contributors")
public class ContributorController {
    
    private final ContributorQueryService contributorQueryService;

    public ContributorController(ContributorQueryService contributorQueryService) {
        this.contributorQueryService = contributorQueryService;
    }

    @GetMapping
    public List<ContributorDto> getContributors() {
        return contributorQueryService.getAllContributors();
    }
}
