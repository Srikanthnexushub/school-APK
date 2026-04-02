package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.port.out.TopicRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTopicRepository extends JpaRepository<Topic, UUID>, TopicRepository {

    @Override
    List<Topic> findByChapterId(UUID chapterId);

    @Override
    Optional<Topic> findByTopicCode(String topicCode);

    @Override
    @Query("SELECT t FROM Topic t WHERE t.id IN :ids")
    List<Topic> findByIds(@Param("ids") List<UUID> ids);
}
