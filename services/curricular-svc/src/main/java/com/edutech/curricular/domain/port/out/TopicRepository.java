package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    Topic save(Topic topic);
    Optional<Topic> findById(UUID id);
    List<Topic> findByChapterId(UUID chapterId);
    Optional<Topic> findByTopicCode(String topicCode);
    List<Topic> findByIds(List<UUID> ids);
}
