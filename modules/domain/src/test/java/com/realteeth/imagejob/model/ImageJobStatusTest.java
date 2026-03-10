package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageJobStatusTest {
    @DisplayName("입력 문자열에 매칭되는 계좌 상태의 Enum을 반환한다.")
    @ParameterizedTest
    @ValueSource(strings = { "ACCEPTED", "accepTEd" })
    void from(String status) {
        // when
        ImageJobStatus result = ImageJobStatus.from(status);

        // then
        assertThat(result).isEqualTo(ImageJobStatus.ACCEPTED);
    }

    @DisplayName("입력 문자열이 유효하지 않으면 예외를 던진다.")
    @ParameterizedTest
    @ValueSource(strings = { "가짜" })
    @NullSource
    void fromWhenInvalid(String input) {
        // when // then
        assertThatThrownBy(() -> ImageJobStatus.from(input))
                .isInstanceOf(DomainException.class)
                .extracting("errorInfo")
                .isEqualTo(ImageJobErrorInfo.INVALID_STATUS);
    }

    @DisplayName("작업 상태가 최종상태(성공/실패)라면 true를 반환한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "SUCCEEDED",
            "FAILED"
    })
    void isTerminalExpectTrue(ImageJobStatus status) {
        // when
        boolean result = status.isTerminal();

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("작업 상태가 최종상태(성공/실패)가 아니라면 false를 반환한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "ACCEPTED",
            "PUBLISH_PENDING",
            "PUBLISHED",
            "DISPATCHING",
            "PROCESSING",
    })
    void isTerminalExpectFalse(ImageJobStatus status) {
        // when
        boolean result = status.isTerminal();

        // then
        assertThat(result).isFalse();
    }
}