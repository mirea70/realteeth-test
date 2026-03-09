package com.realteeth.imagejob.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageJobResult {
    private final String value;
    private final LocalDateTime completedAt;

    public static ImageJobResult of(String value, LocalDateTime completedAt) {
        if(value == null || completedAt == null) {
            return null;
        }

        return new ImageJobResult(value, completedAt);
    }
}
