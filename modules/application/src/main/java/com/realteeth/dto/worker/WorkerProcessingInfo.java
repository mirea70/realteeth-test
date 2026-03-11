package com.realteeth.dto.worker;

public record WorkerProcessingInfo(
        String jobId,
        String status,
        String result,
        Object detail
) {
}
