package com.edutech.aimentor.domain.port.out;

import com.edutech.aimentor.domain.event.DoubtResolvedEvent;
import com.edutech.aimentor.domain.event.DoubtSubmittedEvent;
import com.edutech.aimentor.domain.event.StudyPlanCreatedEvent;

public interface AiMentorEventPublisher {

    void publishStudyPlanCreated(StudyPlanCreatedEvent event);

    void publishDoubtSubmitted(DoubtSubmittedEvent event);

    void publishDoubtResolved(DoubtResolvedEvent event);
}
