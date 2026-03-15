// src/main/java/com/edutech/center/domain/model/SubjectCatalog.java
package com.edutech.center.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical subject catalog and fuzzy-match alias table.
 * Subjects stored in Teacher.subjects must come from this list.
 */
public final class SubjectCatalog {

    public static final List<String> SUBJECTS = List.of(
        "Mathematics", "Physics", "Chemistry", "Biology",
        "English", "Hindi", "History", "Geography",
        "Computer Science", "Social Science", "Economics",
        "Political Science", "Accountancy", "Business Studies",
        "Physical Education", "Fine Arts", "Music"
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("maths",            "Mathematics"),
        Map.entry("math",             "Mathematics"),
        Map.entry("phy",              "Physics"),
        Map.entry("chem",             "Chemistry"),
        Map.entry("bio",              "Biology"),
        Map.entry("cs",               "Computer Science"),
        Map.entry("computers",        "Computer Science"),
        Map.entry("comp sci",         "Computer Science"),
        Map.entry("it",               "Computer Science"),
        Map.entry("sst",              "Social Science"),
        Map.entry("social studies",   "Social Science"),
        Map.entry("pol sci",          "Political Science"),
        Map.entry("eco",              "Economics"),
        Map.entry("accounts",         "Accountancy"),
        Map.entry("bst",              "Business Studies"),
        Map.entry("pe",               "Physical Education"),
        Map.entry("arts",             "Fine Arts")
    );

    private SubjectCatalog() {}

    /** Returns the canonical name if recognized (exact or alias match, case-insensitive). */
    public static Optional<String> resolve(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String normalised = raw.strip();
        for (String s : SUBJECTS) {
            if (s.equalsIgnoreCase(normalised)) return Optional.of(s);
        }
        String alias = ALIASES.get(normalised.toLowerCase());
        return Optional.ofNullable(alias);
    }

    /** Best guess for "did you mean?" suggestions when the subject is not recognised. */
    public static Optional<String> suggest(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String lc = raw.strip().toLowerCase();
        // Try substring containment first
        for (String s : SUBJECTS) {
            if (s.toLowerCase().contains(lc) || lc.contains(s.toLowerCase())) {
                return Optional.of(s);
            }
        }
        // Check aliases substring
        for (Map.Entry<String, String> e : ALIASES.entrySet()) {
            if (e.getKey().contains(lc) || lc.contains(e.getKey())) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }
}
