package com.realteeth.outbox;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.CommonDomainErrorInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED;

    private static final Map<String, OutboxStatus> valueMap =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            status -> status.name().toLowerCase(),
                            status -> status
                    ));

    public static OutboxStatus from(String input) {
        if (input == null) {
            throw new DomainException(CommonDomainErrorInfo.INVALID_OUTBOX_STATUS);
        }

        OutboxStatus result = valueMap.get(input.toLowerCase());
        if (result == null) {
            throw new DomainException(CommonDomainErrorInfo.INVALID_OUTBOX_STATUS);
        }

        return result;
    }
}
