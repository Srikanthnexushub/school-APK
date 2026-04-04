package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemPromptBuilder — role-specific prompt generation")
class SystemPromptBuilderTest {

    private SystemPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SystemPromptBuilder();
    }

    // ─── STUDENT ─────────────────────────────────

    @Test
    @DisplayName("Student prompt contains ERS score, name, and weak areas")
    void student_prompt_contains_ers_and_profile() {
        StudentContext ctx = new StudentContext(
            UUID.randomUUID(), "Ravi Kumar", "Grade 11", "CBSE", "Science",
            List.of("Physics", "Chemistry"), 2026,
            BigDecimal.valueOf(72.5), "ON_TRACK",
            List.of(new WeakAreaSummary("Physics", "Thermodynamics", 45.0, "HIGH")),
            List.of(new MasterySummary("Chemistry", 80.0, "PROFICIENT")),
            Optional.empty(), 3, Optional.empty(), 2,
            Optional.of("Batch A"), Optional.of("NexusEd Center"),
            Optional.empty(), Optional.empty(), Optional.empty(), "dashboard"
        );

        String prompt = builder.build(ctx);

        assertThat(prompt).contains("Ravi Kumar");
        assertThat(prompt).contains("72.5/100");
        assertThat(prompt).contains("ON_TRACK");
        assertThat(prompt).contains("Thermodynamics");
        assertThat(prompt).contains("Physics");
        assertThat(prompt).contains("student");
    }

    @Test
    @DisplayName("Student prompt with empty weak areas shows no critical weak areas message")
    void student_prompt_no_weak_areas() {
        StudentContext ctx = StudentContext.empty(UUID.randomUUID(), "performance");
        String prompt = builder.build(ctx);
        assertThat(prompt).contains("No critical weak areas detected");
    }

    @Test
    @DisplayName("Student prompt with null stream shows 'Not set'")
    void student_prompt_null_stream() {
        StudentContext ctx = StudentContext.empty(UUID.randomUUID(), "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).contains("Not set");
    }

    // ─── TEACHER ─────────────────────────────────

    @Test
    @DisplayName("Teacher prompt contains name, specializations, and teacher role description")
    void teacher_prompt_contains_profile_and_role() {
        TeacherContext ctx = new TeacherContext(
            UUID.randomUUID(), "Priya Sharma", "Expert in advanced calculus",
            List.of("Mathematics", "Statistics"), "NexusEd Center", UUID.randomUUID(), "batches"
        );

        String prompt = builder.build(ctx);

        assertThat(prompt).contains("Priya Sharma");
        assertThat(prompt).contains("Mathematics");
        assertThat(prompt).contains("Statistics");
        assertThat(prompt).contains("Expert in advanced calculus");
        assertThat(prompt).contains("teaching assistant");
        assertThat(prompt).contains("batches");
    }

    @Test
    @DisplayName("Teacher prompt with empty specializations shows 'Not specified'")
    void teacher_prompt_empty_specializations() {
        TeacherContext ctx = TeacherContext.empty(UUID.randomUUID(), null, "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).contains("Not specified");
    }

    @Test
    @DisplayName("Teacher prompt does not contain ERS or student-specific sections")
    void teacher_prompt_no_student_content() {
        TeacherContext ctx = TeacherContext.empty(UUID.randomUUID(), null, "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).doesNotContain("ERS");
        assertThat(prompt).doesNotContain("Exam Readiness Score");
        assertThat(prompt).doesNotContain("WEAK AREAS");
    }

    // ─── ADMIN ───────────────────────────────────

    @Test
    @DisplayName("Admin prompt contains role description and center info")
    void admin_prompt_contains_center_and_role() {
        UUID centerId = UUID.randomUUID();
        AdminContext ctx = new AdminContext(
            UUID.randomUUID(), "Suresh Admin", "NexusEd Center", centerId, "teachers"
        );

        String prompt = builder.build(ctx);

        assertThat(prompt).contains("Suresh Admin");
        assertThat(prompt).contains(centerId.toString());
        assertThat(prompt).contains("operations assistant");
        assertThat(prompt).contains("teacher approvals");
    }

    @Test
    @DisplayName("Admin prompt with null centerId shows platform-wide admin message")
    void admin_prompt_null_center_id() {
        AdminContext ctx = AdminContext.empty(UUID.randomUUID(), null, "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).contains("Platform-wide admin");
    }

    @Test
    @DisplayName("Admin prompt does not contain student academic content")
    void admin_prompt_no_student_content() {
        AdminContext ctx = AdminContext.empty(UUID.randomUUID(), UUID.randomUUID(), "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).doesNotContain("ERS");
        assertThat(prompt).doesNotContain("study plan");
    }

    // ─── PARENT ──────────────────────────────────

    @Test
    @DisplayName("Parent prompt contains name, linked students, and parent role description")
    void parent_prompt_contains_students_and_role() {
        ParentContext ctx = new ParentContext(
            UUID.randomUUID(), "Meena Patel",
            List.of("Arjun Patel", "Kavya Patel"), "fees"
        );

        String prompt = builder.build(ctx);

        assertThat(prompt).contains("Meena Patel");
        assertThat(prompt).contains("Arjun Patel");
        assertThat(prompt).contains("Kavya Patel");
        assertThat(prompt).contains("parent liaison");
        assertThat(prompt).contains("fees");
    }

    @Test
    @DisplayName("Parent prompt with no linked students shows 'No linked students yet'")
    void parent_prompt_no_linked_students() {
        ParentContext ctx = ParentContext.empty(UUID.randomUUID(), "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).contains("No linked students yet");
    }

    @Test
    @DisplayName("Parent prompt does not contain teacher or admin content")
    void parent_prompt_no_teacher_or_admin_content() {
        ParentContext ctx = ParentContext.empty(UUID.randomUUID(), "dashboard");
        String prompt = builder.build(ctx);
        assertThat(prompt).doesNotContain("batch management");
        assertThat(prompt).doesNotContain("teacher approvals");
    }
}
