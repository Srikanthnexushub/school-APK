package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.ActivitySession;
import com.edutech.curricular.domain.port.out.ActivitySessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaActivitySessionRepository extends JpaRepository<ActivitySession, UUID>, ActivitySessionRepository {

    @Override
    List<ActivitySession> findByActivityId(UUID activityId);

    @Override
    List<ActivitySession> findByActivityIdOrderBySessionDateAsc(UUID activityId);
}
