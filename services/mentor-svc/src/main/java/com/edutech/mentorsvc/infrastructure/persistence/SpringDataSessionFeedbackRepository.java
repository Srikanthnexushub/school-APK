package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSessionFeedbackRepository extends JpaRepository<SessionFeedback, UUID> {

    @Query("SELECT f FROM SessionFeedback f WHERE f.session.id = :sessionId")
    Optional<SessionFeedback> findBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM SessionFeedback f WHERE f.session.id = :sessionId")
    boolean existsBySessionId(@Param("sessionId") UUID sessionId);

    List<SessionFeedback> findByMentorId(UUID mentorId);

    List<SessionFeedback> findByStudentId(UUID studentId);
}
