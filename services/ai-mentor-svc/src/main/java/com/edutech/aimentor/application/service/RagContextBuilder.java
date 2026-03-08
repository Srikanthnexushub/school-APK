package com.edutech.aimentor.application.service;

import com.edutech.aimentor.domain.model.DoubtTicket;
import org.springframework.stereotype.Component;

/**
 * Builds a structured RAG (Retrieval-Augmented Generation) prompt context
 * for doubt resolution. Enriches the raw student question with subject-area
 * context, example formats, and answer guidelines before sending to ai-gateway-svc.
 *
 * Keeping prompt engineering logic in the application layer (not domain) ensures
 * that AI concerns remain outside the pure domain model.
 */
@Component
public class RagContextBuilder {

    private static final String SYSTEM_CONTEXT_TEMPLATE = """
            You are EduPath AI Mentor, an expert educational AI assistant for Indian competitive exam preparation.

            Subject Area: %s
            Student Grade Level: Not specified (assume secondary/higher-secondary)

            INSTRUCTIONS:
            1. Provide a clear, step-by-step explanation
            2. Use simple language appropriate for exam preparation
            3. Include a worked example where relevant
            4. Mention any common misconceptions or traps
            5. Keep the answer focused — no unnecessary padding
            6. For MATHEMATICS/PHYSICS/CHEMISTRY: show all steps of calculation
            7. End with a one-line summary of the key concept

            Student Doubt:
            %s
            """;

    /**
     * Build an enriched prompt for the given doubt ticket.
     */
    public String buildPrompt(DoubtTicket doubt) {
        String subject = doubt.getSubjectArea() != null ? doubt.getSubjectArea().name() : "General";
        return SYSTEM_CONTEXT_TEMPLATE.formatted(subject, doubt.getQuestion());
    }

    /**
     * Build a brief follow-up prompt if the initial answer was too vague.
     */
    public String buildFollowUpPrompt(DoubtTicket doubt, String previousAnswer) {
        String subject = doubt.getSubjectArea() != null ? doubt.getSubjectArea().name() : "General";
        return """
                Previous answer provided: %s

                The student needs a clearer explanation. Please:
                1. Simplify the explanation further
                2. Add a concrete numerical example
                3. Break into bullet points if helpful

                Subject: %s
                Question: %s
                """.formatted(previousAnswer, subject, doubt.getQuestion());
    }
}
