package com.edutech.student.application.service;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.domain.model.Board;
import com.edutech.student.domain.model.CollegeProgram;
import com.edutech.student.domain.model.Gender;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.model.Stream;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StudentProfileService — grade range and stream/program tests")
class StudentProfileGradeRangeTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    @InjectMocks
    private StudentProfileService studentProfileService;

    private CreateStudentProfileRequest buildRequest(int grade, Stream stream, CollegeProgram collegeProgram) {
        return new CreateStudentProfileRequest(
                USER_ID,
                "Priya",
                "Kumar",
                "priya." + grade + "@test.com",
                "9000000000",
                Gender.FEMALE,
                LocalDate.of(2010, 6, 1),
                "Chennai",
                "Tamil Nadu",
                null,
                "600001",
                null,
                Board.CBSE,
                grade,
                null,
                null,
                stream,
                collegeProgram
        );
    }

    private StudentProfile buildSavedProfile(int grade, Stream stream, CollegeProgram collegeProgram) {
        StudentProfile profile = StudentProfile.create(
                USER_ID,
                "Priya",
                "Kumar",
                "priya." + grade + "@test.com",
                "9000000000",
                Gender.FEMALE,
                LocalDate.of(2010, 6, 1),
                "Chennai",
                "Tamil Nadu",
                "600001",
                Board.CBSE,
                grade
        );
        if (stream != null) profile.selectStream(stream);
        if (collegeProgram != null) profile.setCollegeProgram(collegeProgram);
        return profile;
    }

    @Test
    @DisplayName("createProfile_grade1_succeeds: class=1 passes @Min(1) validation")
    void createProfile_grade1_succeeds() {
        CreateStudentProfileRequest request = buildRequest(1, null, null);
        StudentProfile saved = buildSavedProfile(1, null, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.currentClass()).isEqualTo(1);
    }

    @Test
    @DisplayName("createProfile_grade12_succeeds: class=12 passes validation")
    void createProfile_grade12_succeeds() {
        CreateStudentProfileRequest request = buildRequest(12, null, null);
        StudentProfile saved = buildSavedProfile(12, null, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.currentClass()).isEqualTo(12);
    }

    @Test
    @DisplayName("createProfile_grade13_succeeds: class=13 (dropper) passes validation")
    void createProfile_grade13_succeeds() {
        CreateStudentProfileRequest request = buildRequest(13, null, null);
        StudentProfile saved = buildSavedProfile(13, null, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.currentClass()).isEqualTo(13);
    }

    @Test
    @DisplayName("createProfile_withStream_setsStreamOnProfile: stream=PCM is set on saved profile")
    void createProfile_withStream_setsStreamOnProfile() {
        CreateStudentProfileRequest request = buildRequest(11, Stream.PCM, null);
        StudentProfile saved = buildSavedProfile(11, Stream.PCM, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(captor.getValue().getStream()).isEqualTo(Stream.PCM);
        assertThat(response.stream()).isEqualTo(Stream.PCM);
    }

    @Test
    @DisplayName("createProfile_withCollegeProgram_setsProgramOnProfile: collegeProgram=BTECH is set")
    void createProfile_withCollegeProgram_setsProgramOnProfile() {
        CreateStudentProfileRequest request = buildRequest(12, null, CollegeProgram.BTECH);
        StudentProfile saved = buildSavedProfile(12, null, CollegeProgram.BTECH);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(captor.getValue().getCollegeProgram()).isEqualTo(CollegeProgram.BTECH);
        assertThat(response.collegeProgram()).isEqualTo(CollegeProgram.BTECH);
    }

    @Test
    @DisplayName("createProfile_schoolStudent_streamNull_profileHasNullStream: stream=null stays null")
    void createProfile_schoolStudent_streamNull_profileHasNullStream() {
        CreateStudentProfileRequest request = buildRequest(8, null, null);
        StudentProfile saved = buildSavedProfile(8, null, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        when(profileRepository.save(captor.capture())).thenReturn(saved);

        studentProfileService.createProfile(request);

        assertThat(captor.getValue().getStream()).isNull();
    }

    @Test
    @DisplayName("createProfile_grade1_boardCBSE_succeeds: primary school student full happy path")
    void createProfile_grade1_boardCBSE_succeeds() {
        CreateStudentProfileRequest request = buildRequest(1, null, null);
        StudentProfile saved = buildSavedProfile(1, null, null);

        when(profileRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

        StudentProfileResponse response = studentProfileService.createProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.currentClass()).isEqualTo(1);
        assertThat(response.board()).isEqualTo(Board.CBSE);
        assertThat(response.stream()).isNull();
        assertThat(response.collegeProgram()).isNull();
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("createProfile_grade10_grade11_grade12_allSucceed: regression — existing grades still work")
    void createProfile_grade10_grade11_grade12_allSucceed() {
        for (int grade : new int[]{10, 11, 12}) {
            String email = "regression." + grade + "@test.com";
            CreateStudentProfileRequest request = new CreateStudentProfileRequest(
                    UUID.randomUUID(),
                    "Test",
                    "User",
                    email,
                    "9111111111",
                    Gender.MALE,
                    LocalDate.of(2005, 1, 1),
                    "Pune",
                    "Maharashtra",
                    null,
                    "411001",
                    null,
                    Board.CBSE,
                    grade,
                    null,
                    null,
                    null,
                    null
            );

            StudentProfile saved = StudentProfile.create(
                    request.userId(),
                    "Test",
                    "User",
                    email,
                    "9111111111",
                    Gender.MALE,
                    LocalDate.of(2005, 1, 1),
                    "Pune",
                    "Maharashtra",
                    "411001",
                    Board.CBSE,
                    grade
            );

            when(profileRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(profileRepository.save(any(StudentProfile.class))).thenReturn(saved);

            StudentProfileResponse response = studentProfileService.createProfile(request);

            assertThat(response).isNotNull();
            assertThat(response.currentClass()).isEqualTo(grade);
        }
    }
}
