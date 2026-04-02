package com.edutech.curricular.application.service;

import com.edutech.curricular.application.dto.*;
import com.edutech.curricular.domain.model.*;
import com.edutech.curricular.domain.port.in.QueryChildJourneyUseCase;
import com.edutech.curricular.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChildJourneyService implements QueryChildJourneyUseCase {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final ActivityEnrollmentRepository enrollmentRepository;
    private final ActivityAchievementRepository achievementRepository;
    private final ActivityRepository activityRepository;
    private final TopicRepository topicRepository;

    @Override
    @Transactional(readOnly = true)
    public ChildJourneyResponse getChildJourney(UUID studentId, UUID parentId) {
        // Learning paths
        List<LearningPath> paths = learningPathRepository.findByStudentId(studentId);
        List<LearningPathResponse> pathResponses = paths.stream()
            .map(path -> {
                List<LearningPathItem> items = learningPathItemRepository.findByPathId(path.getId());
                List<LearningPathItemResponse> itemResponses = items.stream()
                    .map(item -> {
                        String topicTitle = topicRepository.findById(item.getTopicId())
                            .map(Topic::getTopicTitle)
                            .orElse("Unknown");
                        String bloomsLevel = topicRepository.findById(item.getTopicId())
                            .map(t -> t.getBloomsLevel().name())
                            .orElse("REMEMBER");
                        return new LearningPathItemResponse(
                            item.getId(), item.getTopicId(), topicTitle, bloomsLevel,
                            item.getSequenceOrder(), item.getUrgencyScore(), item.getStatus().name()
                        );
                    }).toList();
                return new LearningPathResponse(
                    path.getId(), path.getStudentId(), path.getSubjectId(), path.getGradeId(),
                    path.getComputedAt(), itemResponses
                );
            }).toList();

        // Activity enrollments
        List<ActivityEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<ActivityEnrollmentResponse> enrollmentResponses = enrollments.stream()
            .map(enrollment -> {
                String activityName = activityRepository.findById(enrollment.getActivityId())
                    .map(ExtracurricularActivity::getName)
                    .orElse("Unknown");
                String category = activityRepository.findById(enrollment.getActivityId())
                    .map(a -> a.getCategory().name())
                    .orElse("OTHER");
                return new ActivityEnrollmentResponse(
                    enrollment.getId(), enrollment.getActivityId(), activityName, category,
                    enrollment.getStatus().name(), enrollment.getEnrolledAt()
                );
            }).toList();

        // Achievements
        List<ActivityAchievement> achievements = achievementRepository.findByStudentId(studentId);
        List<AchievementResponse> achievementResponses = achievements.stream()
            .map(achievement -> {
                String activityName = activityRepository.findById(achievement.getActivityId())
                    .map(ExtracurricularActivity::getName)
                    .orElse("Unknown");
                return new AchievementResponse(
                    achievement.getId(), achievement.getStudentId(), achievement.getActivityId(),
                    activityName, achievement.getAchievementType().name(),
                    achievement.getTitle(), achievement.getIssuedAt()
                );
            }).toList();

        return new ChildJourneyResponse(studentId, pathResponses, enrollmentResponses, achievementResponses);
    }
}
