package com.edutech.curricular.application.service;

import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.TopicPrerequisite;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Kahn's algorithm (BFS-based) topological sort for topic prerequisites.
 * Topics with unresolved prerequisites are placed after their prerequisite topics.
 */
@Component
public class TopologicalSortService {

    /**
     * Sort topics in dependency order (prerequisites first).
     *
     * @param topics        list of topics to sort
     * @param prerequisites prerequisite edges (from=must come before, to=depends on from)
     * @return topologically sorted list
     * @throws CircularDependencyException if a cycle is detected
     */
    public List<Topic> sort(List<Topic> topics, List<TopicPrerequisite> prerequisites) {
        if (topics == null || topics.isEmpty()) {
            return Collections.emptyList();
        }

        // Build id -> topic map
        Map<UUID, Topic> topicMap = new HashMap<>();
        for (Topic t : topics) {
            topicMap.put(t.getId(), t);
        }

        // inDegree: how many prerequisites each topic has
        Map<UUID, Integer> inDegree = new HashMap<>();
        // adjacency: from -> list of topics that depend on it
        Map<UUID, List<UUID>> adjacency = new HashMap<>();

        for (Topic t : topics) {
            inDegree.put(t.getId(), 0);
            adjacency.put(t.getId(), new ArrayList<>());
        }

        for (TopicPrerequisite prereq : prerequisites) {
            UUID from = prereq.getFromTopicId(); // must be done first
            UUID to = prereq.getToTopicId();     // depends on from

            // Only count edges between topics in our set
            if (topicMap.containsKey(from) && topicMap.containsKey(to)) {
                adjacency.get(from).add(to);
                inDegree.merge(to, 1, Integer::sum);
            }
        }

        // BFS: start with all topics that have no prerequisites
        Queue<UUID> queue = new LinkedList<>();
        for (Map.Entry<UUID, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Topic> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            result.add(topicMap.get(current));

            for (UUID neighbor : adjacency.get(current)) {
                int remaining = inDegree.merge(neighbor, -1, Integer::sum);
                if (remaining == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (result.size() < topics.size()) {
            throw new CircularDependencyException(
                "Circular dependency detected in topic prerequisites. " +
                result.size() + " of " + topics.size() + " topics resolved."
            );
        }

        return result;
    }
}
