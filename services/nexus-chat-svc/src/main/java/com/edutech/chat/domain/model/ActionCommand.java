package com.edutech.chat.domain.model;

import java.util.Map;

public record ActionCommand(
    String action,
    Map<String, Object> params
) {
    public static final String CREATE_STUDY_PLAN  = "CREATE_STUDY_PLAN";
    public static final String SCHEDULE_REMINDER  = "SCHEDULE_REMINDER";
    public static final String SHOW_WEAK_AREAS    = "SHOW_WEAK_AREAS";
    public static final String SHOW_FEES          = "SHOW_FEES";
    public static final String NAVIGATE           = "NAVIGATE";
    public static final String ENROLL_EXAM        = "ENROLL_EXAM";
}
