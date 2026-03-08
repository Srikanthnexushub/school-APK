package com.edutech.auth.domain.port.out;

/**
 * Port: stores and retrieves single-use CAPTCHA challenge answers.
 */
public interface CaptchaChallengeStore {

    /** Persist challenge answer with a short TTL. */
    void save(String id, String answer);

    /**
     * Atomically fetch the stored answer and delete it (single-use).
     * Returns null if expired or not found.
     */
    String findAndDelete(String id);
}
