// src/main/java/com/edutech/center/application/dto/InvitationLookupResponse.java
package com.edutech.center.application.dto;

import java.util.UUID;

public record InvitationLookupResponse(
    UUID teacherId,
    UUID centerId,
    String centerName,
    String email,
    String firstName,
    String lastName
) {}
