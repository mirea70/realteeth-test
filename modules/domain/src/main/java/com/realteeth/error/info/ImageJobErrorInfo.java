package com.realteeth.error.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageJobErrorInfo implements ErrorInfo {
    // ImageJob
    NOT_FOUND("IMAGEJOB_NOT_FOUND", "이미지 작업을 찾을 수 없습니다."),

    // ImageJobId
    ID_NOT_EXIST("ID_NOT_EXIST", "이미지 작업의 시스템ID 값이 비어있을 수 없습니다."),
    ID_NOT_POSITIVE("ID_NOT_POSITIVE", "이미지 작업의 시스템ID 값은 양의 정수여야 합니다."),

    // ImageJobStatus
    INVALID_STATUS("INVALID_IMAGEJOB_STATUS", "유효하지 않은 이미지 작업 상태입니다.");

    private final String code;
    private final String message;
}
