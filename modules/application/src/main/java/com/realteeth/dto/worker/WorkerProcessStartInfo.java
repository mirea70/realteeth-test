package com.realteeth.dto.worker;

public record WorkerProcessStartInfo(
        String jobId,
        String status,
        Object detail
) {
}
