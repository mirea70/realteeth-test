package com.realteeth.error.exception;

import com.realteeth.error.info.ErrorInfo;
import lombok.Getter;

import java.util.Map;

/**
 * DomainException : 도메인 규칙에 위배되었을 때 발생
 */

@Getter
public class DomainException extends RuntimeException {
    private final ErrorInfo errorInfo;
    private final Map<String, Object> details;

    public DomainException(ErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = Map.of();
    }

    public DomainException(ErrorInfo errorInfo, Object... args) {
        super(String.format(errorInfo.getMessage(), args));
        this.errorInfo = errorInfo;
        this.details = Map.of();
    }

    public DomainException(ErrorInfo errorInfo, Map<String, Object> details) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = details == null ? Map.of() : details;
    }
}
