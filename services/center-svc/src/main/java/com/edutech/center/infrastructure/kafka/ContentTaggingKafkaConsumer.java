package com.edutech.center.infrastructure.kafka;

import com.edutech.center.domain.event.ContentUploadedEvent;
import com.edutech.center.domain.model.ContentItem;
import com.edutech.center.domain.model.TaggingResult;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Listens for {@link ContentUploadedEvent} on the center-events Kafka topic.
 * Calls the AI tagging port to auto-classify the document, then saves the
 * result and transitions status to AVAILABLE.
 *
 * On any error the failure is logged and the item remains PROCESSING
 * (admins can manually trigger re-tagging via the /ai-tag endpoint).
 */
@Component
public class ContentTaggingKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContentTaggingKafkaConsumer.class);

    private final AiTaggingPort aiTaggingPort;
    private final ContentRepository contentRepository;

    public ContentTaggingKafkaConsumer(AiTaggingPort aiTaggingPort,
                                       ContentRepository contentRepository) {
        this.aiTaggingPort = aiTaggingPort;
        this.contentRepository = contentRepository;
    }

    @KafkaListener(topics = "${kafka.topics.center-events}",
                   groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onContentUploaded(Object rawEvent) {
        if (!(rawEvent instanceof ContentUploadedEvent event)) {
            return; // ignore other center events
        }
        log.debug("Auto-tagging content: id={} title='{}'", event.contentId(), event.title());
        try {
            Optional<ContentItem> opt = contentRepository.findByIdActive(event.contentId());
            if (opt.isEmpty()) {
                log.warn("Content not found for auto-tagging: id={}", event.contentId());
                return;
            }
            ContentItem item = opt.get();
            TaggingResult result = aiTaggingPort.suggestTags(event.title(), null);
            item.applyAiTags(result.aiSummary(), result.subject(), result.board(),
                    result.examType(), result.difficulty(), result.suggestedTags());
            contentRepository.save(item);
            log.info("Auto-tagging complete: id={} subject={} examType={}",
                    event.contentId(), result.subject(), result.examType());
        } catch (Exception e) {
            log.error("Auto-tagging failed for contentId={} — item stays PROCESSING",
                    event.contentId(), e);
        }
    }
}
