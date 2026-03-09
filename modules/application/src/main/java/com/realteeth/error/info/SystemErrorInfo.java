package com.realteeth.error.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemErrorInfo implements ErrorInfo {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버에서 알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
