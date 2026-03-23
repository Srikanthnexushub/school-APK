package com.edutech.center.domain.model;

/**
 * Result returned by AI auto-tagging.
 * All fields are nullable — the AI may not populate every field.
 * Callers must handle empty/null values gracefully.
 */
public record TaggingResult(
        String subject,
        String board,
        String examType,
        String difficulty,
        String aiSummary,
        String[] suggestedTags
) {
    /** Returns a completely empty result — used as a graceful degradation fallback. */
    public static TaggingResult empty() {
        return new TaggingResult(null, null, null, null, null, new String[0]);
    }

    public boolean isEmpty() {
        return subject == null && board == null && examType == null
                && difficulty == null && aiSummary == null
                && (suggestedTags == null || suggestedTags.length == 0);
    }
}
