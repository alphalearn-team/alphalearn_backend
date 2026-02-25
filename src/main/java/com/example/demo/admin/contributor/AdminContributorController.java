package com.example.demo.admin.contributor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import com.example.demo.contributor.dto.ContributorDto;
import com.example.demo.contributor.dto.DemoteContributorsRequest;
import com.example.demo.contributor.dto.PromoteContributorsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contributors")
public class AdminContributorController {

    private final AdminContributorFacade contributorAdminFacade;

    public AdminContributorController(AdminContributorFacade contributorAdminFacade) {
        this.contributorAdminFacade = contributorAdminFacade;
    }

    @GetMapping
    public List<ContributorDto> getContributors() {
        return contributorAdminFacade.getAllContributors();
    }

    @PostMapping("/promote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ContributorDto> promoteLearners(@RequestBody PromoteContributorsRequest request) {
        return contributorAdminFacade.promoteLearners(request);
    }

    @DeleteMapping("/demote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void demoteContributors(@RequestBody DemoteContributorsRequest request) {
        contributorAdminFacade.demoteContributors(request);
    }
}
