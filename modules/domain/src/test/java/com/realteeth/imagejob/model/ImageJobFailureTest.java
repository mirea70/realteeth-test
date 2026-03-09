package com.realteeth.imagejob.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImageJobFailureTest {

    @DisplayName("필요 요소가 모두 포함되어 있다면, ImageJobFailure 객체가 잘 생성된다.")
    @Test
    void factory() {
        // given
        Integer failureCode = 300;
        String failureMessage = "실패입니다.";
        LocalDateTime now = LocalDateTime.now();

        // when
        ImageJobFailure result = ImageJobFailure.of(failureCode, failureMessage, now);

        // then
        assertThat(result.getCode()).isEqualTo(failureCode);
        assertThat(result.getMessage()).isEqualTo(failureMessage);
        assertThat(result.getFailedAt()).isEqualTo(now);
    }

    @DisplayName("필요 요소 중 하나라도 null이라면, ImageJobFailure는 null을 반환한다.")
    @ParameterizedTest
    @CsvSource(value = {
            "null, 실패, 2026-03-09T18:48:00.557702300",
            "300, null, 2026-03-09T18:48:00.557702300",
            "300, 실패, null",
    }, nullValues = "null")
    void factoryWhenInvalid(Integer failureCode, String failureMessage, LocalDateTime failureAt) {
        // when
        ImageJobFailure result = ImageJobFailure.of(failureCode, failureMessage, failureAt);

        // then
        assertThat(result).isNull();
    }
}