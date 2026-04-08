package com.example.demo.quest.learner;

import org.springframework.stereotype.Component;

import com.example.demo.quest.weekly.WeeklyQuestChallengeSubmissionTag;

@Component
public class QuestChallengeTaggedFriendDtoMapper {

    public QuestChallengeTaggedFriendDto toDto(WeeklyQuestChallengeSubmissionTag tag) {
        return new QuestChallengeTaggedFriendDto(
                tag.getTaggedLearner().getPublicId(),
                tag.getTaggedLearner().getUsername()
        );
    }
}
