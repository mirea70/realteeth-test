package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum ImageJobStatus {
    ACCEPTED("작업 요청 수락 및 Job 생성 완료"),
    PUBLISH_PENDING("작업 위임 처리 메시지 발행 대기"),
    PUBLISHED("작업 위임 처리 메시지 발행 완료"),
    DISPATCHING("외부 Worker 호출 시도 중"),
    PROCESSING("외부 Worker에 작업 위임 완료, 실제 이미지 처리 중"),
    SUCCEEDED("최종 성공"),
    FAILED("최종 실패");

    private final String description;

    private static final Map<String, ImageJobStatus> valueMap =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            status -> status.name().toLowerCase(),
                            status -> status
                    ));

    public static ImageJobStatus from(String input) {
        if (input == null) {
            throw new DomainException(ImageJobErrorInfo.INVALID_STATUS);
        }

        ImageJobStatus result = valueMap.get(input.toLowerCase());
        if (result == null) {
            throw new DomainException(ImageJobErrorInfo.INVALID_STATUS);
        }

        return result;
    }
}
