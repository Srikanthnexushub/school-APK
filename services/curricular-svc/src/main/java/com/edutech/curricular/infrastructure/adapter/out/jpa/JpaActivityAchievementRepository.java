package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.ActivityAchievement;
import com.edutech.curricular.domain.port.out.ActivityAchievementRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaActivityAchievementRepository extends JpaRepository<ActivityAchievement, UUID>, ActivityAchievementRepository {

    @Override
    List<ActivityAchievement> findByStudentId(UUID studentId);

    @Override
    List<ActivityAchievement> findByActivityIdAndStudentId(UUID activityId, UUID studentId);
}
