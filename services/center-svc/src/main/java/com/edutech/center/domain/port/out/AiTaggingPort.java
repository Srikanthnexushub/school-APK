package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.TaggingResult;

/**
 * Port for AI-powered auto-tagging of content library items.
 *
 * Implementations call the ai-gateway-svc and parse the response.
 * On any error the implementation MUST return {@link TaggingResult#empty()}
 * so the upload flow is never blocked by AI unavailability.
 */
public interface AiTaggingPort {

    /**
     * Suggests metadata tags for a content item based on its title and description.
     *
     * @param title       the document title
     * @param description optional description (may be null)
     * @return a {@link TaggingResult} with AI-suggested fields; never null
     */
    TaggingResult suggestTags(String title, String description);
}
