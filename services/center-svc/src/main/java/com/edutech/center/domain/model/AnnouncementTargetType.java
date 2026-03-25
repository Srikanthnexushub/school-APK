// src/main/java/com/edutech/center/domain/model/AnnouncementTargetType.java
package com.edutech.center.domain.model;

public enum AnnouncementTargetType {
    BATCH,   // specific batch (targetId = batchId)
    CENTER,  // entire center
    ROLE,    // all users of a specific role in the center
    ALL      // every user in the center regardless of role
}
