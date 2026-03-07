package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.MentorSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataMentorSessionRepository extends JpaRepository<MentorSession, UUID> {

    @Query("SELECT s FROM MentorSession s WHERE s.mentor.id = :mentorId AND s.deletedAt IS NULL ORDER BY s.scheduledAt ASC")
    List<MentorSession> findByMentorIdAndNotDeleted(@Param("mentorId") UUID mentorId);

    @Query("SELECT s FROM MentorSession s WHERE s.studentId = :studentId AND s.deletedAt IS NULL ORDER BY s.scheduledAt ASC")
    List<MentorSession> findByStudentIdAndNotDeleted(@Param("studentId") UUID studentId);
}
