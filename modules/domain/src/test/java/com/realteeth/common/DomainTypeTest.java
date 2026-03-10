package com.realteeth.common;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.CommonDomainErrorInfo;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DomainTypeTest {

    @DisplayName("입력 문자열에 매칭되는 계좌 상태의 Enum을 반환한다.")
    @ParameterizedTest
    @ValueSource(strings = { "IMAGE_JOB", "IMAge_jOB" })
    void from(String status) {
        // when
        DomainType result = DomainType.from(status);

        // then
        assertThat(result).isEqualTo(DomainType.IMAGE_JOB);
    }

    @DisplayName("입력 문자열이 유효하지 않으면 예외를 던진다.")
    @ParameterizedTest
    @ValueSource(strings = { "가짜" })
    @NullSource
    void fromWhenInvalid(String input) {
        // when // then
        assertThatThrownBy(() -> DomainType.from(input))
                .isInstanceOf(DomainException.class)
                .extracting("errorInfo")
                .isEqualTo(CommonDomainErrorInfo.INVALID_DOMAIN_TYPE);
    }
}