package com.realteeth.service;

import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import com.realteeth.service.imagejob.ImageJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ImageJobServiceTest {

    @Mock
    private ImageJobPersistenceOutport imageJobPersistenceOutport;

    @InjectMocks
    private ImageJobService imageJobService;

    @Test
    @DisplayName("readOne: requestImageJobId에 해당하는 ImageJob이 존재하면 ImageJobResponse를 반환한다")
    void readOne_success() {
        // given
        Long requestImageJobId = 1L;
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 12, 0, 0);
        String sourceImageUrl = "https://example.com/source.png";

        ImageJob imageJob = ImageJob.createNew(
                requestImageJobId,
                sourceImageUrl,
                now
        );

        given(imageJobPersistenceOutport.loadOne(any(ImageJobId.class)))
                .willReturn(Optional.of(imageJob));

        // when
        ImageJobResponse response = imageJobService.readOne(requestImageJobId);

        // then
        assertThat(response).isNotNull();

        assertThat(response.imageJobId()).isEqualTo(requestImageJobId);
        assertThat(response.sourceImageUrl()).isEqualTo(sourceImageUrl);
        assertThat(response.createdAt()).isEqualTo(now);

        then(imageJobPersistenceOutport)
                .should(times(1))
                .loadOne(any(ImageJobId.class));
    }

    @Test
    @DisplayName("readOne: requestImageJobId에 해당하는 ImageJob이 없으면 BusinessException을 던진다")
    void readOne_notFound() {
        // given
        Long requestImageJobId = 999L;

        given(imageJobPersistenceOutport.loadOne(any(ImageJobId.class)))
                .willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> imageJobService.readOne(requestImageJobId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class);

        BusinessException exception = (BusinessException) thrown;
        assertThat(exception.getErrorInfo()).isEqualTo(ImageJobErrorInfo.NOT_FOUND);

        then(imageJobPersistenceOutport)
                .should(times(1))
                .loadOne(any(ImageJobId.class));
    }
}