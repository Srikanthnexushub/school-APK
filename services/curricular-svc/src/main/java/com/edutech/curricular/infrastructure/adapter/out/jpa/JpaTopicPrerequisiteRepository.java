package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.TopicPrerequisite;
import com.edutech.curricular.domain.port.out.TopicPrerequisiteRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTopicPrerequisiteRepository extends JpaRepository<TopicPrerequisite, UUID>, TopicPrerequisiteRepository {

    @Override
    List<TopicPrerequisite> findByToTopicId(UUID toTopicId);

    @Override
    List<TopicPrerequisite> findByFromTopicId(UUID fromTopicId);

    @Override
    @Query("SELECT tp FROM TopicPrerequisite tp WHERE tp.fromTopicId IN " +
           "(SELECT t.id FROM Topic t JOIN Chapter c ON t.chapterId = c.id " +
           "JOIN Subject s ON c.subjectId = s.id WHERE s.id = :subjectId)")
    List<TopicPrerequisite> findBySubjectId(@Param("subjectId") UUID subjectId);
}
