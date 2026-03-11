package com.realteeth.error.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemErrorInfo implements ErrorInfo {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버에서 알 수 없는 오류가 발생했습니다."),
    WORKER_ISSUE_KEY_FAIL("WORKER_ISSUE_KEY_FAIL", "Worker 시스템으로부터 액세스 키 발급에 실패하였습니다."),
    WORKER_PROCESS_START_FAIL("WORKER_PROCESS_START_FAIL", "Worker 시스템으로 이미지 작업 위임에 실패하였습니다.");

    private final String code;
    private final String message;
}
