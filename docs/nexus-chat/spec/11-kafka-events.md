# NexusChat — Kafka Events

## Topics Consumed by nexus-chat-svc

### 1. `assess-events` → ExamSubmittedEvent

Published by: `assess-svc`

```java
// Event shape (from assess-svc):
{
  "eventType": "EXAM_SUBMITTED",
  "studentId": "bc46d073-...",
  "examId": "...",
  "examTitle": "Physics Unit Test 3",
  "scoredMarks": 28.0,
  "totalMarks": 50.0,
  "percentage": 56.0,
  "letterGrade": "C",
  "submittedAt": "2026-04-01T10:00:00Z"
}
```

**nexus-chat-svc action**: Generate proactive nudge → publish to `notification-send` topic.

---

### 2. `performance-events` → WeakAreaDetectedEvent (CRITICAL only)

Published by: `performance-svc`

```java
// Event shape (from performance-svc):
{
  "eventType": "WEAK_AREA_DETECTED",
  "studentId": "bc46d073-...",
  "subject": "Physics",
  "topic": "Thermodynamics",
  "masteryPercent": 32.5,
  "severity": "CRITICAL"   // Only trigger nudge if severity == CRITICAL
}
```

**nexus-chat-svc action**: Generate proactive nudge only when `severity == CRITICAL`.

---

## Topics Published by nexus-chat-svc

### 1. `nexus-chat-events`

**Schema** (for audit/analytics):
```json
{
  "eventType": "CHAT_MESSAGE_SENT",
  "sessionId": "...",
  "userId": "...",
  "userRole": "STUDENT",
  "messageRole": "USER",
  "tokenCount": 24,
  "latencyMs": 850,
  "timestamp": "2026-04-01T10:01:00Z"
}
```

### 2. `notification-send`

**Schema** (same as existing notification-svc contract):
```json
{
  "userId": "bc46d073-...",
  "type": "IN_APP",
  "title": "Nexus AI Insight",
  "message": "⚠️ Your Thermodynamics score dropped to critical! Let's fix this together.",
  "actionUrl": "/chat?nudge=a1b2c3d4-...",
  "referenceId": "a1b2c3d4-...",
  "referenceType": "PROACTIVE_NUDGE"
}
```

---

## Kafka Consumer Config (in application.yml)

```yaml
spring:
  kafka:
    consumer:
      group-id: nexus-chat-svc
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.edutech.*"
        spring.json.use.type.headers: false
```

## Kafka Listener Code (summary)

```java
@KafkaListener(topics = "${kafka.topics.assess-events}", groupId = "nexus-chat-svc")
public void onAssessEvent(@Payload Map<String, Object> event) {
    String eventType = (String) event.get("eventType");
    if ("EXAM_SUBMITTED".equals(eventType)) {
        proactiveNudgeService.handleExamSubmitted(event);
    }
}

@KafkaListener(topics = "${kafka.topics.performance-events}", groupId = "nexus-chat-svc")
public void onPerformanceEvent(@Payload Map<String, Object> event) {
    String eventType = (String) event.get("eventType");
    String severity = (String) event.get("severity");
    if ("WEAK_AREA_DETECTED".equals(eventType) && "CRITICAL".equals(severity)) {
        proactiveNudgeService.handleWeakAreaCritical(event);
    }
}
```

## .env Kafka additions

```bash
# nexus-chat-svc Kafka topics (add to .env)
NEXUS_CHAT_EVENTS_TOPIC=nexus-chat-events
# Already exists in .env:
# ASSESS_EVENTS_TOPIC=assess-events
# PERFORMANCE_EVENTS_TOPIC=performance-events
# NOTIFICATION_SEND_TOPIC=notification-send
```
