// src/test/java/com/edutech/parent/application/service/StudentLinkServiceTest.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.StudentLinkResponse;
import com.edutech.parent.application.exception.DuplicateStudentLinkException;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.application.exception.StudentLinkNotFoundException;
import com.edutech.parent.domain.model.LinkStatus;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.model.Role;
import com.edutech.parent.domain.model.StudentLink;
import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import com.edutech.parent.domain.port.out.StudentLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentLinkService Unit Tests")
class StudentLinkServiceTest {

    private static final UUID PARENT_USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID     = UUID.randomUUID();
    private static final UUID CENTER_ID      = UUID.randomUUID();

    @Mock
    private ParentProfileRepository profileRepository;

    @Mock
    private StudentLinkRepository linkRepository;

    @Mock
    private ParentEventPublisher eventPublisher;

    @InjectMocks
    private StudentLinkService studentLinkService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AuthPrincipal callerPrincipal() {
        return new AuthPrincipal(PARENT_USER_ID, "parent@test.com", Role.PARENT, null, "fp");
    }

    private AuthPrincipal otherPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "other@test.com", Role.PARENT, null, "fp");
    }

    private ParentProfile parentProfile() {
        ParentProfile profile = mock(ParentProfile.class);
        when(profile.getUserId()).thenReturn(PARENT_USER_ID);
        return profile;
    }

    private LinkStudentRequest linkRequest() {
        return new LinkStudentRequest(UUID.randomUUID(), "Test Student", CENTER_ID, null, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // linkStudent tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("linkStudent_success: valid parent, not already linked → returns StudentLinkResponse with ACTIVE status and publishes event")
    void linkStudent_success() {
        // arrange
        ParentProfile profile = parentProfile();
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(linkRepository.findByParentIdAndStudentId(any(), any())).thenReturn(Optional.empty());
        when(linkRepository.save(any(StudentLink.class))).thenAnswer(i -> i.getArgument(0));

        // act
        StudentLinkResponse response = studentLinkService.linkStudent(PROFILE_ID, linkRequest(), callerPrincipal());

        // assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(LinkStatus.ACTIVE);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("linkStudent_parentNotFound: unknown profileId → throws ParentProfileNotFoundException")
    void linkStudent_parentNotFound() {
        // arrange
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> studentLinkService.linkStudent(PROFILE_ID, linkRequest(), callerPrincipal()))
            .isInstanceOf(ParentProfileNotFoundException.class);

        verify(linkRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("linkStudent_accessDenied: different user tries to link → throws ParentAccessDeniedException")
    void linkStudent_accessDenied() {
        // arrange
        ParentProfile profile = parentProfile(); // userId = PARENT_USER_ID
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        // act & assert — caller is otherPrincipal whose userId != PARENT_USER_ID
        assertThatThrownBy(() -> studentLinkService.linkStudent(PROFILE_ID, linkRequest(), otherPrincipal()))
            .isInstanceOf(ParentAccessDeniedException.class);

        verify(linkRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("linkStudent_duplicate: student already ACTIVE linked → throws DuplicateStudentLinkException")
    void linkStudent_duplicate() {
        // arrange
        ParentProfile profile = parentProfile();
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        StudentLink existingLink = mock(StudentLink.class);
        when(existingLink.getStatus()).thenReturn(LinkStatus.ACTIVE);
        when(linkRepository.findByParentIdAndStudentId(any(), any())).thenReturn(Optional.of(existingLink));

        // act & assert
        assertThatThrownBy(() -> studentLinkService.linkStudent(PROFILE_ID, linkRequest(), callerPrincipal()))
            .isInstanceOf(DuplicateStudentLinkException.class);

        verify(linkRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // revokeLink tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("revokeLink_success: valid link found, parent owns it → link is REVOKED and event published")
    void revokeLink_success() {
        // arrange — use a real StudentLink created via the factory method
        StudentLink link = StudentLink.create(PROFILE_ID, UUID.randomUUID(), "Student", CENTER_ID);

        ParentProfile parent = parentProfile();
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(parent));
        when(linkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // act
        studentLinkService.revokeLink(link.getId(), callerPrincipal());

        // assert
        assertThat(link.getStatus()).isEqualTo(LinkStatus.REVOKED);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("revokeLink_notFound: unknown linkId → throws StudentLinkNotFoundException")
    void revokeLink_notFound() {
        // arrange
        UUID unknownLinkId = UUID.randomUUID();
        when(linkRepository.findById(unknownLinkId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> studentLinkService.revokeLink(unknownLinkId, callerPrincipal()))
            .isInstanceOf(StudentLinkNotFoundException.class);

        verify(linkRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
