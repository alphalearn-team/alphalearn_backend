package com.example.demo.contributor.dto;

import java.util.List;
import java.util.UUID;

public record PromoteContributorsRequest(
        List<UUID> learnerIds
) {}
