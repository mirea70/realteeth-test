package com.realteeth.port.out;

import com.realteeth.dto.worker.WorkerProcessingInfo;

public interface WorkerOutport {
    String getApiKey();
    String processStart(String apiKey, String imageUrl);
    WorkerProcessingInfo getProcessingInfo(String jobId);
}
