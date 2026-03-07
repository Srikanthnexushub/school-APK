// src/main/java/com/edutech/assess/application/dto/SubmitAnswersRequest.java
package com.edutech.assess.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAnswersRequest(@NotEmpty @Valid List<AnswerEntry> answers) {}
