// src/main/java/com/edutech/parent/application/dto/UpdateParentProfileRequest.java
package com.edutech.parent.application.dto;

public record UpdateParentProfileRequest(
        String name, String phone, String email,
        String address, String city, String district, String state, String country, String pincode,
        String relationshipType, String occupation, String gender
) {}
