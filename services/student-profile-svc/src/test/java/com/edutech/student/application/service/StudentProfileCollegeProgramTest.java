package com.edutech.student.application.service;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.UpdateStudentProfileRequest;
import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.CollegeProgram;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudentProfileService — CollegeProgram field tests")
class StudentProfileCollegeProgramTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    @InjectMocks
    private StudentProfileService studentProfileService;

    private CreateStudentProfileRequest buildCreateRequest(CollegeProgram program) {
        return new CreateStudentProfileRequest(
                USER_ID,
                "Rohan",
                "Mehta",
                "rohan.college@test.com",
                "9800000001",
                Gender.MALE,
                LocalDate.of(2003, 8, 15),
                "Bengaluru",
                "Karnataka",
                null,
                "560001",
                null,
                Board.CBSE,
                12,
                null,
                null,
                null,
                program
        );
    }

    private StudentProfile buildSavedProfile(CollegeProgram program) {
        StudentProfile profile = StudentProfile.create(
                USER_ID,
                "Rohan",
                "Mehta",
                "rohan.college@test.com",
                "9800000001",
                Gender.MALE,
                LocalDate.of(2003, 8, 15),
                "Bengaluru",
                "Karnataka",
                "560001",
                Board.CBSE,
                12
        );
        if (program != null) profile.setCollegeProgram(program);
        return profile;
    }

    @Test
    @DisplayName("createProfile_btech_setsProgramCorrectly: college student with BTECH program")
    void createProfile_btech_setsProgramCorrectly() {
        CreateStudentProfileRequest request = buildCreateRequest(CollegeProgram.BTECH);
        StudentProfile saved = buildSavedProfile(CollegeProgram.BTECH);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(captor.getValue().getCollegeProgram()).isEqualTo(CollegeProgram.BTECH);
        assertThat(response.collegeProgram()).isEqualTo(CollegeProgram.BTECH);
    }

    @Test
    @DisplayName("createProfile_mbbs_setsProgramCorrectly: college student with MBBS program")
    void createProfile_mbbs_setsProgramCorrectly() {
        CreateStudentProfileRequest request = buildCreateRequest(CollegeProgram.MBBS);
        StudentProfile saved = buildSavedProfile(CollegeProgram.MBBS);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(captor.getValue().getCollegeProgram()).isEqualTo(CollegeProgram.MBBS);
        assertThat(response.collegeProgram()).isEqualTo(CollegeProgram.MBBS);
    }

    @Test
    @DisplayName("createProfile_nullProgram_profileHasNullProgram: no program set means null")
    void createProfile_nullProgram_profileHasNullProgram() {
        CreateStudentProfileRequest request = buildCreateRequest(null);
        StudentProfile saved = buildSavedProfile(null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(captor.getValue().getCollegeProgram()).isNull();
        assertThat(response.collegeProgram()).isNull();
    }

    @Test
    @DisplayName("collegeProgram_appearsInResponse: toResponse() includes collegeProgram correctly")
    void collegeProgram_appearsInResponse() {
        CreateStudentProfileRequest request = buildCreateRequest(CollegeProgram.BCA);
        StudentProfile saved = buildSavedProfile(CollegeProgram.BCA);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(response.collegeProgram()).isEqualTo(CollegeProgram.BCA);
    }

    @Test
    @DisplayName("updateProfile_canChangeCollegeProgram: updateProfile with new program changes it")
    void updateProfile_canChangeCollegeProgram() {
        StudentProfile existingProfile = buildSavedProfile(CollegeProgram.BTECH);

        UpdateStudentProfileRequest updateRequest = new UpdateStudentProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CollegeProgram.MBA
        );

        when(profileRepository.findById(STUDENT_ID)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentProfileResponse response = studentProfileService.updateProfile(STUDENT_ID, updateRequest);

        assertThat(response.collegeProgram()).isEqualTo(CollegeProgram.MBA);
    }
}
