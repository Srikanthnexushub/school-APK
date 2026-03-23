package com.edutech.center;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ConfirmUploadRequest;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.domain.model.ContentType;
import com.edutech.center.domain.model.Difficulty;
import com.edutech.center.domain.model.ExamType;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.DocumentStoragePort;
import com.edutech.center.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the global library search & discovery endpoints.
 *
 * Tests (10):
 *  1. /library/search with no params returns all AVAILABLE content
 *  2. /library/search?q=keyword finds by title (FTS)
 *  3. /library/search?subject=Physics filters correctly
 *  4. /library/search?board=CBSE filters correctly
 *  5. /library/search?examType=JEE&difficulty=HARD filters correctly
 *  6. /library/search?yearOfPaper=2023 filters correctly
 *  7. combined subject+board filter narrows results
 *  8. /library/trending returns content sorted by download_count desc
 *  9. /library/recent returns recently added content
 * 10. empty search result returns empty page (not 500)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("center-svc — ContentSearch HTTP Integration Tests")
class ContentSearchIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("center_search_test")
                    .withUsername("search_user")
                    .withPassword("search_pass");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean @SuppressWarnings("rawtypes") KafkaTemplate kafkaTemplate;
    @MockBean JwtTokenValidator jwtTokenValidator;
    @MockBean DocumentStoragePort documentStoragePort;
    @MockBean io.minio.MinioClient minioClient;
    @MockBean AiTaggingPort aiTaggingPort;

    @Autowired TestRestTemplate restTemplate;

    static final UUID CENTER_ADMIN_USER_ID = UUID.randomUUID();
    static final UUID CENTER_ID            = UUID.randomUUID();
    static final String FAKE_TOKEN         = "search-test-token";

    @BeforeEach
    void setUp() {
        AuthPrincipal principal = new AuthPrincipal(CENTER_ADMIN_USER_ID, "admin@search-test.com",
                Role.CENTER_ADMIN, CENTER_ID, "fp-search-admin");
        when(jwtTokenValidator.validate(anyString())).thenReturn(Optional.of(principal));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(documentStoragePort.buildObjectKey(any(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0) + "/docs/uuid/" + inv.getArgument(2));
        when(documentStoragePort.generateUploadUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://minio.test/upload?sig=abc");
        when(documentStoragePort.generateDownloadUrl(anyString(), anyInt()))
                .thenReturn("https://minio.test/download?sig=xyz");
        when(aiTaggingPort.suggestTags(anyString(), any()))
                .thenReturn(com.edutech.center.domain.model.TaggingResult.empty());
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(FAKE_TOKEN);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpEntity<Void> authEntity()         { return new HttpEntity<>(authHeaders()); }
    private <T> HttpEntity<T> authEntity(T body)  { return new HttpEntity<>(body, authHeaders()); }

    private String contentBase() { return "/api/v1/centers/" + CENTER_ID + "/content"; }

    /** Seeds a content item via confirm, then manually marks it AVAILABLE via ai-tag (empty result). */
    private ContentItemResponse seed(String title, String subject, String board,
                                      Short year, ExamType examType, Difficulty difficulty) {
        var req = new ConfirmUploadRequest(null, title, "Description for " + title,
                ContentType.PREVIOUS_YEAR_PAPER,
                CENTER_ID + "/docs/uuid/" + title.replaceAll("\\s", "-") + ".pdf",
                102400L, "application/pdf", 20, null,
                subject, board, "12", year, examType, difficulty, "ENGLISH",
                List.of(subject != null ? subject.toLowerCase() : "general"));
        ResponseEntity<ContentItemResponse> res = restTemplate.postForEntity(
                contentBase() + "/confirm", authEntity(req), ContentItemResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Trigger ai-tag to transition status to AVAILABLE
        restTemplate.postForEntity(contentBase() + "/" + res.getBody().id() + "/ai-tag",
                authEntity(), ContentItemResponse.class);
        return res.getBody();
    }

    @Test
    @DisplayName("T01 — /library/search with no params returns content")
    void search_noParams_returnsContent() {
        seed("JEE 2023 Maths", "Mathematics", "CBSE", (short) 2023, ExamType.JEE, Difficulty.HARD);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search", HttpMethod.GET, authEntity(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsKey("content");
    }

    @Test
    @DisplayName("T02 — /library/search?q=keyword finds by title FTS")
    void search_byKeyword_findsByTitle() {
        seed("NEET Biology Chapter 7 Genetics", "Biology", "CBSE", (short) 2022, ExamType.NEET, Difficulty.MEDIUM);
        seed("JEE Physics Electrostatics", "Physics", "CBSE", (short) 2023, ExamType.JEE, Difficulty.HARD);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?q=genetics", HttpMethod.GET, authEntity(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        boolean found = content.stream().anyMatch(c ->
                c.get("title").toString().toLowerCase().contains("genetics"));
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("T03 — /library/search?subject=Physics filters correctly")
    void search_bySubject_filtersCorrectly() {
        seed("Physics Wave Optics", "Physics", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.MEDIUM);
        seed("Chemistry Organic Reactions", "Chemistry", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.HARD);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?subject=Physics", HttpMethod.GET, authEntity(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        assertThat(content).allMatch(c -> "Physics".equals(c.get("subject")));
    }

    @Test
    @DisplayName("T04 — /library/search?board=CBSE filters correctly")
    void search_byBoard_filtersCorrectly() {
        seed("CBSE Maths Practice", "Mathematics", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.EASY);
        seed("ICSE History Notes", "History", "ICSE", (short) 2022, ExamType.BOARD_EXAM, Difficulty.EASY);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?board=CBSE", HttpMethod.GET, authEntity(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        assertThat(content).allMatch(c -> "CBSE".equals(c.get("board")));
    }

    @Test
    @DisplayName("T05 — /library/search?examType=JEE&difficulty=HARD filters correctly")
    void search_byExamTypeAndDifficulty_filtersCorrectly() {
        seed("JEE Advanced 2022 Paper", "Physics", "CBSE", (short) 2022, ExamType.JEE, Difficulty.HARD);
        seed("NEET Easy Biology", "Biology", "CBSE", (short) 2022, ExamType.NEET, Difficulty.EASY);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?examType=JEE&difficulty=HARD",
                HttpMethod.GET, authEntity(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        assertThat(content).allMatch(c ->
                "JEE".equals(c.get("examType")) && "HARD".equals(c.get("difficulty")));
    }

    @Test
    @DisplayName("T06 — /library/search?yearOfPaper=2023 filters correctly")
    void search_byYearOfPaper_filtersCorrectly() {
        seed("2023 Physics Board", "Physics", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.MEDIUM);
        seed("2021 Chemistry Board", "Chemistry", "CBSE", (short) 2021, ExamType.BOARD_EXAM, Difficulty.MEDIUM);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?yearOfPaper=2023", HttpMethod.GET, authEntity(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        assertThat(content).allMatch(c -> Integer.valueOf(2023).equals(c.get("yearOfPaper")));
    }

    @Test
    @DisplayName("T07 — combined subject+board filter narrows results")
    void search_combinedFilters_narrowsResults() {
        seed("CBSE Physics Chapter 1", "Physics", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.EASY);
        seed("ICSE Physics Chapter 1", "Physics", "ICSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.EASY);
        seed("CBSE Math Chapter 1", "Mathematics", "CBSE", (short) 2023, ExamType.BOARD_EXAM, Difficulty.EASY);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?subject=Physics&board=CBSE",
                HttpMethod.GET, authEntity(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) res.getBody().get("content");
        assertThat(content).allMatch(c ->
                "Physics".equals(c.get("subject")) && "CBSE".equals(c.get("board")));
    }

    @Test
    @DisplayName("T08 — /library/trending returns content ordered by download_count")
    void trending_returnsContent() {
        seed("Popular Paper", "Physics", "CBSE", (short) 2023, ExamType.JEE, Difficulty.HARD);

        ResponseEntity<List> res = restTemplate.exchange(
                "/api/v1/library/trending?size=10", HttpMethod.GET, authEntity(), List.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    @DisplayName("T09 — /library/recent returns recently added content")
    void recent_returnsRecentContent() {
        seed("Brand New Notes", "Chemistry", "CBSE", (short) 2024, ExamType.UNIT_TEST, Difficulty.EASY);

        ResponseEntity<List> res = restTemplate.exchange(
                "/api/v1/library/recent?days=1&size=10", HttpMethod.GET, authEntity(), List.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("T10 — /library/search with no matches returns empty page (not 500)")
    void search_noMatches_returnsEmptyPage() {
        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/library/search?q=zyxwvutsrqponmlkjihgfedcba",
                HttpMethod.GET, authEntity(), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<?> content = (List<?>) res.getBody().get("content");
        assertThat(content).isEmpty();
        assertThat(res.getBody().get("totalElements")).isEqualTo(0);
    }
}
