package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageJobIdTest {
    @DisplayName("객체 생성 시, 이미지 작업의 Key 값이 존재하고 양수이면 잘 생성된다.")
    @Test
    void factory() {
        // given
        Long input = 2L;

        // when
        ImageJobId result = new ImageJobId(input);

        // then
        long expected = 2L;

        assertThat(result.getValue()).isEqualTo(expected);
    }

    @DisplayName("유효하지 않은 값이 들어오면 예외를 던진다.")
    @ParameterizedTest
    @MethodSource("imageJobIdInvalidCases")
    void factoryWithLongWhenInvalid(Long input, ImageJobErrorInfo errorInfo) {
        // when // then
        assertThatThrownBy(() -> new ImageJobId(input))
                .isInstanceOf(DomainException.class)
                .extracting("errorInfo")
                .isEqualTo(errorInfo);
    }

    private static Stream<Arguments> imageJobIdInvalidCases() {
        return Stream.of(
                Arguments.of(
                        null,
                        ImageJobErrorInfo.ID_NOT_EXIST
                ),
                Arguments.of(
                        0L,
                        ImageJobErrorInfo.ID_NOT_POSITIVE
                ),
                Arguments.of(
                        -1L,
                        ImageJobErrorInfo.ID_NOT_POSITIVE
                )
        );
    }
}