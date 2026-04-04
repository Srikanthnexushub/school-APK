// src/main/java/com/edutech/assess/domain/model/ExamMode.java
package com.edutech.assess.domain.model;

public enum ExamMode {
    ONLINE,
    OFFLINE,
    STANDARD,  // legacy — migrated to ONLINE
    CAT        // legacy — migrated to OFFLINE
}
