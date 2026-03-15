// src/main/java/com/edutech/center/application/service/TeacherBulkImportService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BulkImportConfirmResponse;
import com.edutech.center.application.dto.BulkImportPreviewResponse;
import com.edutech.center.application.dto.BulkRowError;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.event.TeacherInvitationEvent;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.SubjectCatalog;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TeacherBulkImportService {

    private static final Logger log = LoggerFactory.getLogger(TeacherBulkImportService.class);
    private static final int MAX_ROWS = 500;
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    private final TeacherRepository teacherRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;

    public TeacherBulkImportService(TeacherRepository teacherRepository,
                                     CenterRepository centerRepository,
                                     CenterEventPublisher eventPublisher) {
        this.teacherRepository = teacherRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Validate CSV and return a preview. No DB writes. */
    @Transactional(readOnly = true)
    public BulkImportPreviewResponse preview(UUID centerId, MultipartFile file, AuthPrincipal principal) {
        assertAccess(centerId, principal);
        centerRepository.findById(centerId).orElseThrow(() -> new CenterNotFoundException(centerId));

        List<CsvRow> rows = parseCsv(file);
        List<BulkRowError> errors = validateRows(rows, centerId, new HashSet<>());

        int valid = rows.size() - errors.stream().map(BulkRowError::row).distinct().toList().size();
        return new BulkImportPreviewResponse(rows.size(), valid, rows.size() - valid, errors);
    }

    /**
     * Validate CSV again and import valid rows.
     * If skipErrors=true, invalid rows are silently skipped; otherwise the whole
     * batch is rejected when errors remain.
     */
    @Transactional
    public BulkImportConfirmResponse confirm(UUID centerId, MultipartFile file,
                                              boolean skipErrors, AuthPrincipal principal) {
        assertAccess(centerId, principal);
        CoachingCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new CenterNotFoundException(centerId));

        List<CsvRow> rows = parseCsv(file);
        Set<Integer> errorRows = new HashSet<>();
        List<BulkRowError> errors = validateRows(rows, centerId, errorRows);

        if (!errors.isEmpty() && !skipErrors) {
            throw new IllegalArgumentException(
                "CSV has " + errors.size() + " error(s). Fix them or re-submit with skipErrors=true.");
        }

        Instant tokenExpiry = Instant.now().plus(7, ChronoUnit.DAYS);
        List<Teacher> toSave = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            if (errorRows.contains(i + 1)) continue;
            CsvRow row = rows.get(i);

            String token = UUID.randomUUID().toString();
            Teacher stub = Teacher.createInvitationStub(
                centerId, row.firstName(), row.lastName(), row.email(),
                row.phone(), row.subjects(), row.employeeId(), token, tokenExpiry);
            toSave.add(stub);
        }

        teacherRepository.saveAll(toSave);

        // Publish invitation events (best-effort; Kafka failure doesn't roll back)
        for (Teacher t : toSave) {
            eventPublisher.publish(new TeacherInvitationEvent(
                t.getId(), centerId, center.getName(),
                t.getEmail(), t.getFirstName(), t.getLastName(), t.getInvitationToken()));
        }

        log.info("Bulk import complete: centerId={} imported={} skipped={}",
            centerId, toSave.size(), rows.size() - toSave.size());
        return new BulkImportConfirmResponse(toSave.size(), rows.size() - toSave.size(),
            "Invitation emails are being sent to " + toSave.size() + " teachers.");
    }

    /** Resolve an invitation token (public — no auth). */
    @Transactional(readOnly = true)
    public Optional<Teacher> findByToken(String token) {
        return teacherRepository.findByInvitationToken(token)
            .filter(t -> t.getInvitationTokenExpiresAt() != null
                && t.getInvitationTokenExpiresAt().isAfter(Instant.now()));
    }

    /** Link teacher stub to an auth-svc user after they complete registration. */
    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        Teacher teacher = teacherRepository.findByInvitationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation token"));
        if (teacher.getInvitationTokenExpiresAt() == null ||
            teacher.getInvitationTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invitation token has expired");
        }
        teacher.acceptInvitation(userId);
        teacherRepository.save(teacher);
        log.info("Invitation accepted: teacherId={} userId={}", teacher.getId(), userId);
    }

    // ─── CSV Parsing ──────────────────────────────────────────────────────────

    private List<CsvRow> parseCsv(MultipartFile file) {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.toLowerCase().contains("first name")) continue; // skip header
                if (line.isBlank()) continue;
                if (rows.size() >= MAX_ROWS) break;
                String[] cols = splitCsvLine(line);
                rows.add(new CsvRow(
                    col(cols, 0), col(cols, 1), col(cols, 2),
                    col(cols, 3), col(cols, 4), col(cols, 5)
                ));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }
        return rows;
    }

    private String col(String[] cols, int idx) {
        if (idx >= cols.length) return "";
        return cols[idx].strip().replaceAll("^\"|\"$", "");
    }

    private String[] splitCsvLine(String line) {
        // Handles quoted fields containing commas
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(sb.toString()); sb = new StringBuilder(); }
            else { sb.append(c); }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private List<BulkRowError> validateRows(List<CsvRow> rows, UUID centerId, Set<Integer> errorRowNumbers) {
        List<BulkRowError> errors = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 1;
            CsvRow row = rows.get(i);
            boolean hasError = false;

            if (row.firstName().isBlank()) {
                errors.add(new BulkRowError(rowNum, row.email(), "firstName", "First name is required"));
                hasError = true;
            }
            if (row.lastName().isBlank()) {
                errors.add(new BulkRowError(rowNum, row.email(), "lastName", "Last name is required"));
                hasError = true;
            }
            if (row.email().isBlank()) {
                errors.add(new BulkRowError(rowNum, "", "email", "Email is required"));
                hasError = true;
            } else if (!EMAIL_PATTERN.matcher(row.email()).matches()) {
                errors.add(new BulkRowError(rowNum, row.email(), "email", "Invalid email format"));
                hasError = true;
            } else if (seenEmails.contains(row.email().toLowerCase())) {
                errors.add(new BulkRowError(rowNum, row.email(), "email", "Duplicate email in file"));
                hasError = true;
            } else if (teacherRepository.existsByEmailAndCenterId(row.email(), centerId)
                    || teacherRepository.existsByEmail(row.email())) {
                errors.add(new BulkRowError(rowNum, row.email(), "email", "Email already registered"));
                hasError = true;
            } else {
                seenEmails.add(row.email().toLowerCase());
            }

            // Validate subjects
            if (!row.subjects().isBlank()) {
                String[] subs = row.subjects().split(",");
                for (String sub : subs) {
                    String trimmed = sub.strip();
                    if (SubjectCatalog.resolve(trimmed).isEmpty()) {
                        Optional<String> suggestion = SubjectCatalog.suggest(trimmed);
                        String msg = "Subject '" + trimmed + "' not recognized";
                        String hint = suggestion.map(s -> "did you mean '" + s + "'?").orElse(null);
                        errors.add(new BulkRowError(rowNum, row.email(), "subjects", msg, hint));
                        hasError = true;
                    }
                }
            }

            if (hasError) errorRowNumbers.add(rowNum);
        }
        return errors;
    }

    private void assertAccess(UUID centerId, AuthPrincipal principal) {
        if (principal.belongsToCenter(centerId)) return;
        // Allow CENTER_ADMINs whose JWT centerId is still null (Kafka sync pending).
        boolean isOwningAdmin = centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
        if (!isOwningAdmin) throw new CenterAccessDeniedException();
    }

    record CsvRow(String firstName, String lastName, String email,
                  String phone, String subjects, String employeeId) {}
}
