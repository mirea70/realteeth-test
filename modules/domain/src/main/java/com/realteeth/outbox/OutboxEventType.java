package com.realteeth.outbox;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.CommonDomainErrorInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum OutboxEventType {
    DISPATCH,
    POLL;

    private static final Map<String, OutboxEventType> valueMap =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            status -> status.name().toLowerCase(),
                            status -> status
                    ));

    public static OutboxEventType from(String input) {
        if (input == null) {
            throw new DomainException(CommonDomainErrorInfo.INVALID_OUTBOX_TYPE);
        }

        OutboxEventType result = valueMap.get(input.toLowerCase());
        if (result == null) {
            throw new DomainException(CommonDomainErrorInfo.INVALID_OUTBOX_TYPE);
        }

        return result;
    }
}
