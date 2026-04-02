package com.edutech.chat.infrastructure.adapter.out.webclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParentProfileDto(
    String id,
    @JsonProperty("name") String fullName,
    String email,
    List<String> linkedStudentNames
) {
    public static ParentProfileDto empty() {
        return new ParentProfileDto(null, "Parent", null, List.of());
    }
}
