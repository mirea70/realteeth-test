package com.realteeth.error.response;

import com.realteeth.error.info.ErrorInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ErrorResponse {
    private final Instant timestamp;
    private final String code;
    private final String message;
    private final String path;
    private final Map<String, Object> details;

    public static ErrorResponse of(ErrorInfo errorInfo, String path, Map<String, Object> details) {
        return new ErrorResponse(
                Instant.now(),
                errorInfo.getCode(),
                errorInfo.getMessage(),
                path,
                details == null ? Map.of() : details
        );
    }

    public static ErrorResponse of(String code, String message, String path, Map<String, Object> details) {
        return new ErrorResponse(
                Instant.now(),
                code,
                message,
                path,
                details == null ? Map.of() : details
        );
    }
}
