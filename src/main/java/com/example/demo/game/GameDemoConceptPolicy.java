package com.example.demo.game;

import com.example.demo.concept.Concept;
import java.util.List;
import java.util.Set;

public final class GameDemoConceptPolicy {

    private static final Set<Integer> DEMO_CONCEPT_IDS = Set.of(1, 10, 9, 17, 15);

    private GameDemoConceptPolicy() {
    }

    public static List<Concept> filterConcepts(List<Concept> concepts, boolean demoModeEnabled) {
        if (!demoModeEnabled) {
            return concepts;
        }
        return concepts.stream()
                .filter(GameDemoConceptPolicy::isDemoConcept)
                .toList();
    }

    public static boolean isAllowed(Concept concept, boolean demoModeEnabled) {
        if (!demoModeEnabled) {
            return true;
        }
        return isDemoConcept(concept);
    }

    private static boolean isDemoConcept(Concept concept) {
        return concept != null && concept.getConceptId() != null && DEMO_CONCEPT_IDS.contains(concept.getConceptId());
    }
}
