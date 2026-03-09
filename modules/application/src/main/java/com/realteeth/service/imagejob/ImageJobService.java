package com.realteeth.service.imagejob;

import com.realteeth.dto.response.ImageJobResponse;
import com.realteeth.error.exception.BusinessException;
import com.realteeth.error.info.ImageJobErrorInfo;
import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;
import com.realteeth.port.in.ImageJobUseCase;
import com.realteeth.port.out.ImageJobPersistenceOutport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ImageJobService implements ImageJobUseCase {
    private final ImageJobPersistenceOutport imageJobPersistenceOutport;

    @Transactional(readOnly = true)
    public ImageJobResponse readOne(Long requestImageJobId) {
        ImageJob imageJob = imageJobPersistenceOutport.loadOne(new ImageJobId(requestImageJobId))
                .orElseThrow(() -> new BusinessException(ImageJobErrorInfo.NOT_FOUND));

        return ImageJobResponse.from(imageJob);
    }
}
