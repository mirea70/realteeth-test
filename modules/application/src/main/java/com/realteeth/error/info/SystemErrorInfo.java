package com.realteeth.error.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum SystemErrorInfo implements ErrorInfo {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버에서 알 수 없는 오류가 발생했습니다."),
    WORKER_ISSUE_KEY_FAIL("WORKER_ISSUE_KEY_FAIL", "Worker 시스템으로부터 액세스 키 발급에 실패하였습니다."),
    WORKER_PROCESS_START_FAIL("WORKER_PROCESS_START_FAIL", "Worker 시스템으로 이미지 작업 위임에 실패하였습니다."),
    WORKER_GET_PROCESS_INFO_FAIL("WORKER_GET_PROCESS_INFO_FAIL", "Worker 시스템에서 처리중인 이미지 작업 정보 요청에 실패하였습니다."),
    WORKER_INVALID_STATUS("WORKER_INVALID_STATUS", "Worker에서 반환받은 이미지 작업 상태가 유효하지 않은 값입니다.");

    private final String code;
    private final String message;
    private final boolean retryable;

    SystemErrorInfo(String code, String message) {
        this(code, message, false);
    }

    SystemErrorInfo(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }
}
