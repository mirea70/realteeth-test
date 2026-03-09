package com.realteeth.error.exception;

import com.realteeth.error.info.ErrorInfo;
import lombok.Getter;

import java.util.Map;

/**
 * BusinessException : 유스케이스 진행/오케스트레이션/외부 요인으로 인한 실패일 경우 발생
 */

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorInfo errorInfo;
    private final Map<String, Object> details;

    public BusinessException(ErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = Map.of();
    }

    public BusinessException(ErrorInfo errorInfo, Map<String, Object> details) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
        this.details = details == null ? Map.of() : details;
    }
}
