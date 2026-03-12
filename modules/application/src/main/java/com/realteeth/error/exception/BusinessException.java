package com.realteeth.error.exception;

import com.realteeth.error.info.ErrorInfo;
import com.realteeth.error.info.SystemErrorInfo;
import lombok.Getter;

import java.util.Map;

/**
 * BusinessException : 유스케이스 진행/오케스트레이션/외부 요인으로 인한 실패일 경우 발생
 */

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorInfo errorInfo;
    private final Map<String, Object> details;
    private final Boolean retryable;

    public BusinessException(ErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = Map.of();
        this.retryable = null;
    }

    public BusinessException(ErrorInfo errorInfo, Map<String, Object> details) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = details == null ? Map.of() : details;
        this.retryable = null;
    }

    public BusinessException(ErrorInfo errorInfo, Boolean retryable) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = Map.of();
        this.retryable = retryable;
    }

    public BusinessException(ErrorInfo errorInfo, Map<String, Object> details, Boolean retryable) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = details == null ? Map.of() : details;
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        if (retryable != null) {
            return retryable;
        }

        if (errorInfo instanceof SystemErrorInfo systemErrorInfo) {
            return systemErrorInfo.isRetryable();
        }

        return false;
    }
}
