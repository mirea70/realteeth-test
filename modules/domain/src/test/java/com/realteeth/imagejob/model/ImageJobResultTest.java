package com.realteeth.imagejob.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


class ImageJobResultTest {

    @DisplayName("필요 요소가 모두 포함되어 있다면, ImageJobResult가 잘 생성된다.")
    @Test
    void factory() {
        // given
        String resultImageUrl = "https://example.com/files/asdzxdcasd";
        LocalDateTime now = LocalDateTime.now();

        // when
        ImageJobResult result = ImageJobResult.of(resultImageUrl, now);

        // then
        assertThat(result.getValue()).isEqualTo(resultImageUrl);
        assertThat(result.getCompletedAt()).isEqualTo(now);
    }

    @DisplayName("필요 요소 중 하나라도 null이라면, ImageJobResult는 null을 반환한다.")
    @ParameterizedTest
    @CsvSource(value = {
            "null, 2026-03-09T18:48:00.557702300",
            "https://example.com/files/asdzxdcasd, null"
    }, nullValues = "null")
    void factoryWhenInvalid(String resultImageUrl, LocalDateTime resultAt) {
        // when
        ImageJobResult result = ImageJobResult.of(resultImageUrl, resultAt);

        // then
        assertThat(result).isNull();
    }
}