package com.edutech.chat.infrastructure.adapter.out.webclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeacherProfileDto(
    String fullName,
    String bio,
    List<String> specializations,
    String district
) {
    public static TeacherProfileDto empty() {
        return new TeacherProfileDto("Teacher", null, List.of(), null);
    }
}
