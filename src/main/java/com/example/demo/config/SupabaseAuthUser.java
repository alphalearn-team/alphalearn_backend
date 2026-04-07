package com.example.demo.config;

import java.util.UUID;

import com.example.demo.contributor.Contributor;
import com.example.demo.learner.Learner;

public record SupabaseAuthUser(
        UUID userId,
        Learner learner,
        Contributor contributor,
        String email
) {
    public SupabaseAuthUser(UUID userId, Learner learner, Contributor contributor) {
        this(userId, learner, contributor, null);
    }

    public boolean isLearner() {
        return learner != null;
    }

    public boolean isContributor() {
        return contributor != null && contributor.isCurrentContributor();
    }
}
