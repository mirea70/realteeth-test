package com.realteeth.error.handler;

import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ErrorInfo;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.error.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e, HttpServletRequest request) {
        ErrorInfo errorInfo = e.getErrorInfo();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(errorInfo, request.getRequestURI(), e.getDetails())
                );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorInfo errorInfo = e.getErrorInfo();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(errorInfo, request.getRequestURI(), e.getDetails())
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception e, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.of(SystemErrorInfo.INTERNAL_SERVER_ERROR, request.getRequestURI(), Map.of())
                );
    }
}
