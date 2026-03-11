package com.realteeth.worker;

public record WorkerPollEventPayload(
        Long imageJobId,
        String workerJobId
) {}
