package com.edutech.chat.infrastructure.adapter.out.webclient;

public record AssessContextDto(
    ExamItem lastExam,
    int examsThisMonth
) {
    public static AssessContextDto empty() {
        return new AssessContextDto(null, 0);
    }
}
