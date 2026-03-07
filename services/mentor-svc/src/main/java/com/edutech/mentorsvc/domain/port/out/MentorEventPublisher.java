package com.edutech.mentorsvc.domain.port.out;

import com.edutech.mentorsvc.domain.event.FeedbackSubmittedEvent;
import com.edutech.mentorsvc.domain.event.MentorSessionBookedEvent;
import com.edutech.mentorsvc.domain.event.SessionCompletedEvent;

public interface MentorEventPublisher {
    void publishSessionBooked(MentorSessionBookedEvent event);
    void publishSessionCompleted(SessionCompletedEvent event);
    void publishFeedbackSubmitted(FeedbackSubmittedEvent event);
}
