package com.realteeth.workerclient.adapter;

import com.realteeth.dto.worker.IssueKeyInfo;
import com.realteeth.dto.worker.WorkerProcessStartInfo;
import com.realteeth.dto.worker.WorkerProcessingInfo;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.SystemErrorInfo;
import com.realteeth.port.out.WorkerOutport;
import com.realteeth.workerclient.config.WorkerClientProperties;
import com.realteeth.workerclient.dto.request.IssueKeyRequest;
import com.realteeth.workerclient.dto.request.ProcessRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkerClientAdapter implements WorkerOutport {
    private final RestClient restClient;
    private final WorkerClientProperties properties;

    @Override
    public String getApiKey() {
        IssueKeyInfo response = restClient.post()
                .uri("/mock/auth/issue-key")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IssueKeyRequest(
                        properties.candidateName(),
                        properties.email()
                        )
                ).retrieve()
                .body(IssueKeyInfo.class);

        if(response == null || response.apiKey() == null) {
            throw new BusinessException(SystemErrorInfo.WORKER_ISSUE_KEY_FAIL,
                    response != null ? Map.of("details", response.detail()) : Map.of());
        }

        return response.apiKey();
    }

    @Override
    public String processStart(String apiKey, String imageUrl) {
        WorkerProcessStartInfo response = restClient.post()
                .uri("/mock/process")
                .header("X-API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProcessRequest(imageUrl))
                .retrieve()
                .body(WorkerProcessStartInfo.class);

        if(response == null || response.jobId() == null) {
            throw new BusinessException(SystemErrorInfo.WORKER_PROCESS_START_FAIL,
                    response != null ? Map.of("details", response.detail()) : Map.of());
        }

        return response.jobId();
    }

    @Override
    public WorkerProcessingInfo getProcessingInfo(String jobId) {
        WorkerProcessingInfo response = restClient.get()
                .uri("/mock/process/{jobId}", jobId)
                .retrieve()
                .body(WorkerProcessingInfo.class);

        return response;
    }
}
