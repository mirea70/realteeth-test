package com.realteeth.imagejob.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageJobFailure {
    private final Integer code;
    private final String message;
    private final LocalDateTime failedAt;

    public static ImageJobFailure of(Integer code, String message, LocalDateTime failedAt) {
        if(code == null || message == null || failedAt == null) {
            return null;
        }

        return new ImageJobFailure(code, message, failedAt);
    }
}
