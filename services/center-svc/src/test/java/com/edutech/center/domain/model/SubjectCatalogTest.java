// src/test/java/com/edutech/center/domain/model/SubjectCatalogTest.java
package com.edutech.center.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubjectCatalog — resolve and suggest unit tests")
class SubjectCatalogTest {

    // ─── resolve ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolve — exact match returns canonical name (case-sensitive)")
    void resolve_exactCanonical() {
        assertThat(SubjectCatalog.resolve("Mathematics")).contains("Mathematics");
        assertThat(SubjectCatalog.resolve("Physics")).contains("Physics");
        assertThat(SubjectCatalog.resolve("Computer Science")).contains("Computer Science");
        assertThat(SubjectCatalog.resolve("Fine Arts")).contains("Fine Arts");
        assertThat(SubjectCatalog.resolve("Physical Education")).contains("Physical Education");
    }

    @Test
    @DisplayName("resolve — case-insensitive exact match")
    void resolve_caseInsensitiveExact() {
        assertThat(SubjectCatalog.resolve("mathematics")).contains("Mathematics");
        assertThat(SubjectCatalog.resolve("PHYSICS")).contains("Physics");
        assertThat(SubjectCatalog.resolve("computer science")).contains("Computer Science");
        assertThat(SubjectCatalog.resolve("HINDI")).contains("Hindi");
    }

    @Test
    @DisplayName("resolve — alias map returns canonical name")
    void resolve_aliases() {
        assertThat(SubjectCatalog.resolve("maths")).contains("Mathematics");
        assertThat(SubjectCatalog.resolve("math")).contains("Mathematics");
        assertThat(SubjectCatalog.resolve("cs")).contains("Computer Science");
        assertThat(SubjectCatalog.resolve("it")).contains("Computer Science");
        assertThat(SubjectCatalog.resolve("sst")).contains("Social Science");
        assertThat(SubjectCatalog.resolve("social studies")).contains("Social Science");
        assertThat(SubjectCatalog.resolve("pe")).contains("Physical Education");
        assertThat(SubjectCatalog.resolve("bio")).contains("Biology");
        assertThat(SubjectCatalog.resolve("eco")).contains("Economics");
        assertThat(SubjectCatalog.resolve("pol sci")).contains("Political Science");
        assertThat(SubjectCatalog.resolve("accounts")).contains("Accountancy");
        assertThat(SubjectCatalog.resolve("bst")).contains("Business Studies");
        assertThat(SubjectCatalog.resolve("arts")).contains("Fine Arts");
    }

    @Test
    @DisplayName("resolve — unrecognized input returns empty")
    void resolve_unrecognized_empty() {
        assertThat(SubjectCatalog.resolve("Astrology")).isEmpty();
        assertThat(SubjectCatalog.resolve("xyz")).isEmpty();
        assertThat(SubjectCatalog.resolve("unknown subject here")).isEmpty();
    }

    @Test
    @DisplayName("resolve — blank or null returns empty")
    void resolve_blankOrNull_empty() {
        assertThat(SubjectCatalog.resolve("")).isEmpty();
        assertThat(SubjectCatalog.resolve("   ")).isEmpty();
        assertThat(SubjectCatalog.resolve(null)).isEmpty();
    }

    // ─── suggest ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("suggest — typo with substring match returns closest subject")
    void suggest_substringMatch_returnsSubject() {
        // "mathss" contains "maths" alias → Mathematics
        assertThat(SubjectCatalog.suggest("Mathss")).contains("Mathematics");
        // "physic" contained in "Physics"
        assertThat(SubjectCatalog.suggest("physic")).isPresent();
        // "science" contained in at least one subject
        assertThat(SubjectCatalog.suggest("science")).isPresent();
    }

    @Test
    @DisplayName("suggest — completely unrecognizable returns empty")
    void suggest_noMatch_empty() {
        assertThat(SubjectCatalog.suggest("xyz123")).isEmpty();
        assertThat(SubjectCatalog.suggest("qqqq")).isEmpty();
    }

    @Test
    @DisplayName("suggest — blank or null returns empty")
    void suggest_blankOrNull_empty() {
        assertThat(SubjectCatalog.suggest(null)).isEmpty();
        assertThat(SubjectCatalog.suggest("")).isEmpty();
        assertThat(SubjectCatalog.suggest("  ")).isEmpty();
    }
}
