package com.realteeth.error.info;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonDomainErrorInfo implements ErrorInfo {
    // DomainType
    INVALID_DOMAIN_TYPE("INVALID_DOMAIN_TYPE", "유효하지 않은 도메인 종류입니다."),

    // Outbox
    INVALID_OUTBOX_TYPE("INVALID_OUTBOX_TYPE", "유효하지 않은 아웃박스 이벤트 종류입니다."),
    INVALID_OUTBOX_STATUS("INVALID_OUTBOX_STATUS", "유효하지 않은 아웃박스 이벤트 상태입니다.");

    private final String code;
    private final String message;
}
