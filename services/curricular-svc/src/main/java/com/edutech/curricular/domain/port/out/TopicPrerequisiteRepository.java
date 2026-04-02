package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.TopicPrerequisite;

import java.util.List;
import java.util.UUID;

public interface TopicPrerequisiteRepository {
    TopicPrerequisite save(TopicPrerequisite prerequisite);
    List<TopicPrerequisite> findByToTopicId(UUID toTopicId);
    List<TopicPrerequisite> findByFromTopicId(UUID fromTopicId);
    List<TopicPrerequisite> findBySubjectId(UUID subjectId);
}
