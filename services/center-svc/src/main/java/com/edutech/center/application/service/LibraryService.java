package com.edutech.center.application.service;

import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.domain.port.out.ContentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LibraryService {

    private final ContentRepository contentRepository;
    private final ContentService contentService;

    public LibraryService(ContentRepository contentRepository, ContentService contentService) {
        this.contentRepository = contentRepository;
        this.contentService = contentService;
    }

    @Transactional(readOnly = true)
    public Page<ContentItemResponse> search(String query, String subject, String board,
                                             String classGrade, Short yearOfPaper,
                                             String examType, String difficulty,
                                             String language, String contentType,
                                             UUID centerId, Pageable pageable) {
        return contentRepository.search(
                blankToNull(query), blankToNull(subject), blankToNull(board),
                blankToNull(classGrade), yearOfPaper,
                blankToNull(examType), blankToNull(difficulty),
                blankToNull(language), blankToNull(contentType),
                centerId, pageable
        ).map(contentService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponse> trending(Pageable pageable) {
        return contentRepository.findTrending(pageable)
                .stream().map(contentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponse> recent(int daysBack, Pageable pageable) {
        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);
        return contentRepository.findRecentSince(since, pageable)
                .stream().map(contentService::toResponse).toList();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
