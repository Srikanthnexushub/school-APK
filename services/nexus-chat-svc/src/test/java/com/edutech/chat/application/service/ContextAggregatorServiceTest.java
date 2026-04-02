package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.infrastructure.adapter.out.webclient.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContextAggregatorService — role-based context aggregation")
class ContextAggregatorServiceTest {

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID CENTER_ID = UUID.randomUUID();
    private static final String JWT     = "test-jwt";

    @Mock StudentProfileWebClientAdapter profileClient;
    @Mock PerformanceWebClientAdapter performanceClient;
    @Mock AiMentorWebClientAdapter mentorClient;
    @Mock AssessWebClientAdapter assessClient;
    @Mock CenterWebClientAdapter centerClient;
    @Mock TeacherProfileWebClientAdapter teacherProfileClient;
    @Mock ParentProfileWebClientAdapter parentProfileClient;

    @InjectMocks
    ContextAggregatorService service;

    // ─── STUDENT ─────────────────────────────────

    @Test
    @DisplayName("STUDENT role — aggregates all 5 services and returns StudentContext")
    void student_role_returns_student_context() {
        StudentProfileDto profile = new StudentProfileDto(
            USER_ID, "Ravi Kumar", "Grade 11", "CBSE", "Science",
            List.of("Physics"), 2026
        );
        when(profileClient.fetchProfile(USER_ID, JWT))
            .thenReturn(Mono.just(profile));
        when(performanceClient.fetchPerformance(USER_ID, JWT))
            .thenReturn(Mono.just(PerformanceDto.empty()));
        when(mentorClient.fetchMentorContext(USER_ID, JWT))
            .thenReturn(Mono.just(MentorContextDto.empty()));
        when(assessClient.fetchAssessContext(USER_ID, JWT))
            .thenReturn(Mono.just(AssessContextDto.empty()));
        when(centerClient.fetchCenterContext(USER_ID, JWT))
            .thenReturn(Mono.just(CenterContextDto.empty()));

        RoleContext result = service.aggregate(USER_ID, "STUDENT", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(StudentContext.class);
        assertThat(result.fullName()).isEqualTo("Ravi Kumar");
        assertThat(result.role()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("STUDENT role — all downstream services fail, returns StudentContext.empty()")
    void student_role_all_services_fail_returns_empty() {
        when(profileClient.fetchProfile(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("timeout")));
        when(performanceClient.fetchPerformance(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("timeout")));
        when(mentorClient.fetchMentorContext(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("timeout")));
        when(assessClient.fetchAssessContext(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("timeout")));
        when(centerClient.fetchCenterContext(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("timeout")));

        RoleContext result = service.aggregate(USER_ID, "STUDENT", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(StudentContext.class);
        assertThat(result.fullName()).isEqualTo("Student");
    }

    // ─── TEACHER ─────────────────────────────────

    @Test
    @DisplayName("TEACHER role — fetches mentor profile, returns TeacherContext")
    void teacher_role_returns_teacher_context() {
        TeacherProfileDto profile = new TeacherProfileDto(
            "Priya Sharma", "Expert teacher", List.of("Math", "Science"), "Chennai"
        );
        when(teacherProfileClient.fetchProfile(USER_ID, JWT))
            .thenReturn(Mono.just(profile));

        RoleContext result = service.aggregate(USER_ID, "TEACHER", CENTER_ID, "batches", JWT);

        assertThat(result).isInstanceOf(TeacherContext.class);
        TeacherContext ctx = (TeacherContext) result;
        assertThat(ctx.fullName()).isEqualTo("Priya Sharma");
        assertThat(ctx.specializations()).containsExactly("Math", "Science");
        assertThat(ctx.centerId()).isEqualTo(CENTER_ID);
        assertThat(ctx.role()).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("TEACHER role — mentor-svc fails, returns TeacherContext.empty()")
    void teacher_role_service_fails_returns_empty() {
        when(teacherProfileClient.fetchProfile(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("mentor-svc down")));

        RoleContext result = service.aggregate(USER_ID, "TEACHER", CENTER_ID, "dashboard", JWT);

        assertThat(result).isInstanceOf(TeacherContext.class);
        assertThat(result.fullName()).isEqualTo("Teacher");
    }

    // ─── ADMIN ───────────────────────────────────

    @Test
    @DisplayName("CENTER_ADMIN role — returns AdminContext with centerId from JWT")
    void center_admin_role_returns_admin_context() {
        RoleContext result = service.aggregate(USER_ID, "CENTER_ADMIN", CENTER_ID, "teachers", JWT);

        assertThat(result).isInstanceOf(AdminContext.class);
        AdminContext ctx = (AdminContext) result;
        assertThat(ctx.centerId()).isEqualTo(CENTER_ID);
        assertThat(ctx.role()).isEqualTo("CENTER_ADMIN");
    }

    @Test
    @DisplayName("INSTITUTION_ADMIN role — returns AdminContext with null centerId")
    void institution_admin_role_returns_admin_context_no_center() {
        RoleContext result = service.aggregate(USER_ID, "INSTITUTION_ADMIN", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(AdminContext.class);
        AdminContext ctx = (AdminContext) result;
        assertThat(ctx.centerId()).isNull();
    }

    // ─── PARENT ──────────────────────────────────

    @Test
    @DisplayName("PARENT role — fetches parent profile, returns ParentContext")
    void parent_role_returns_parent_context() {
        ParentProfileDto profile = new ParentProfileDto(
            UUID.randomUUID().toString(), "Meena Patel", "meena@test.com",
            List.of("Arjun Patel")
        );
        when(parentProfileClient.fetchProfile(USER_ID, JWT))
            .thenReturn(Mono.just(profile));

        RoleContext result = service.aggregate(USER_ID, "PARENT", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(ParentContext.class);
        ParentContext ctx = (ParentContext) result;
        assertThat(ctx.fullName()).isEqualTo("Meena Patel");
        assertThat(ctx.linkedStudentNames()).containsExactly("Arjun Patel");
        assertThat(ctx.role()).isEqualTo("PARENT");
    }

    @Test
    @DisplayName("PARENT role — parent-svc fails, returns ParentContext.empty()")
    void parent_role_service_fails_returns_empty() {
        when(parentProfileClient.fetchProfile(any(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("parent-svc down")));

        RoleContext result = service.aggregate(USER_ID, "PARENT", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(ParentContext.class);
        assertThat(result.fullName()).isEqualTo("Parent");
    }

    // ─── UNKNOWN ROLE ────────────────────────────

    @Test
    @DisplayName("Unknown role — falls back to student context aggregation")
    void unknown_role_falls_back_to_student() {
        when(profileClient.fetchProfile(any(), anyString()))
            .thenReturn(Mono.just(StudentProfileDto.empty(USER_ID)));
        when(performanceClient.fetchPerformance(any(), anyString()))
            .thenReturn(Mono.just(PerformanceDto.empty()));
        when(mentorClient.fetchMentorContext(any(), anyString()))
            .thenReturn(Mono.just(MentorContextDto.empty()));
        when(assessClient.fetchAssessContext(any(), anyString()))
            .thenReturn(Mono.just(AssessContextDto.empty()));
        when(centerClient.fetchCenterContext(any(), anyString()))
            .thenReturn(Mono.just(CenterContextDto.empty()));

        RoleContext result = service.aggregate(USER_ID, "SOME_UNKNOWN_ROLE", null, "dashboard", JWT);

        assertThat(result).isInstanceOf(StudentContext.class);
    }
}
