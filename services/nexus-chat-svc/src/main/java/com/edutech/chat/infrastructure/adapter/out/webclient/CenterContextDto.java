package com.edutech.chat.infrastructure.adapter.out.webclient;

public record CenterContextDto(
    String batchName,
    String centerName
) {
    public static CenterContextDto empty() {
        return new CenterContextDto(null, null);
    }
}
