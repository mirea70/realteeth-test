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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class WorkerClientAdapter implements WorkerOutport {
    private final RestClient restClient;
    private final WorkerClientProperties properties;

    @Override
    public String getApiKey() {
        IssueKeyInfo response = execute(
                () -> restClient.post()
                        .uri("/mock/auth/issue-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new IssueKeyRequest(
                                properties.candidateName(),
                                properties.email()
                        ))
                        .retrieve()
                        .body(IssueKeyInfo.class),
                SystemErrorInfo.WORKER_ISSUE_KEY_FAIL
        );

        validateResponse(
                response,
                response != null ? response.apiKey() : null,
                response != null ? String.valueOf(response.detail()) : null,
                SystemErrorInfo.WORKER_ISSUE_KEY_FAIL
        );

        return response.apiKey();
    }

    @Override
    public String processStart(String apiKey, String imageUrl) {
        WorkerProcessStartInfo response = execute(
                () -> restClient.post()
                        .uri("/mock/process")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ProcessRequest(imageUrl))
                        .retrieve()
                        .body(WorkerProcessStartInfo.class),
                SystemErrorInfo.WORKER_PROCESS_START_FAIL
        );

        validateResponse(
                response,
                response != null ? response.jobId() : null,
                response != null ? String.valueOf(response.detail()) : null,
                SystemErrorInfo.WORKER_PROCESS_START_FAIL
        );

        return response.jobId();
    }

    @Override
    public WorkerProcessingInfo getProcessingInfo(String jobId) {
        WorkerProcessingInfo response = execute(
                () -> restClient.get()
                        .uri("/mock/process/{jobId}", jobId)
                        .retrieve()
                        .body(WorkerProcessingInfo.class),
                SystemErrorInfo.WORKER_GET_PROCESS_INFO_FAIL
        );

        validateResponse(
                response,
                response != null ? response.jobId() : null,
                response != null ? String.valueOf(response.detail()) : null,
                SystemErrorInfo.WORKER_GET_PROCESS_INFO_FAIL
        );

        return response;
    }

    private <T> T execute(Supplier<T> call, SystemErrorInfo errorInfo) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw new BusinessException(
                    errorInfo,
                    Map.of(
                            "status", e.getStatusCode().value(),
                            "responseBody", e.getResponseBodyAsString()
                    )
            );
        } catch (RestClientException e) {
            throw new BusinessException(
                    errorInfo,
                    Map.of("details", e.getMessage())
            );
        }
    }

    private void validateResponse(
            Object response,
            Object requiredValue,
            String detail,
            SystemErrorInfo errorInfo
    ) {
        if (response == null || requiredValue == null) {
            throw new BusinessException(
                    errorInfo,
                    detail != null ? Map.of("details", detail) : Map.of()
            );
        }
    }
}
