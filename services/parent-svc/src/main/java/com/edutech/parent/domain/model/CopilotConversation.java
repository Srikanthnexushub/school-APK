package com.edutech.parent.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent conversation history for Parent Copilot — the AI chatbot
 * that helps parents understand their child's academic progress, fees,
 * attendance, and next steps.
 *
 * Each CopilotConversation belongs to one parentId and optionally references
 * a studentId. Messages are stored as an ordered list of CopilotMessage.
 */
@Entity
@Table(name = "copilot_conversations", schema = "parent_schema")
public class CopilotConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConversationStatus status;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<CopilotMessage> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CopilotConversation() {}

    public CopilotConversation(String parentId, String studentId, String title) {
        this.parentId = parentId;
        this.studentId = studentId;
        this.title = title;
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addMessage(String role, String content) {
        CopilotMessage message = new CopilotMessage(this, role, content);
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getParentId() { return parentId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public ConversationStatus getStatus() { return status; }
    public List<CopilotMessage> getMessages() { return Collections.unmodifiableList(messages); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum ConversationStatus { ACTIVE, CLOSED }
}
