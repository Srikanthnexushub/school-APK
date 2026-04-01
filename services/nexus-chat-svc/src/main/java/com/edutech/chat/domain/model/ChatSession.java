package com.edutech.chat.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(schema = "chat_schema", name = "chat_sessions")
@Getter @Setter @NoArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @Column(name = "page_context", length = 100)
    private String pageContext;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatSession create(UUID userId, String userRole, String pageContext) {
        ChatSession s = new ChatSession();
        s.userId = userId;
        s.userRole = userRole;
        s.pageContext = pageContext;
        return s;
    }

    public void setTitleFromFirstMessage(String content) {
        if (this.title == null && content != null && !content.isBlank()) {
            this.title = content.substring(0, Math.min(60, content.length())).trim();
        }
    }

    public void recordMessage() {
        this.messageCount++;
        this.lastActiveAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = SessionStatus.ARCHIVED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return SessionStatus.ACTIVE.equals(this.status) && this.deletedAt == null;
    }
}
