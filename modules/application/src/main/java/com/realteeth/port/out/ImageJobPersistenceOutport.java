package com.realteeth.port.out;

import com.realteeth.imagejob.model.ImageJob;
import com.realteeth.imagejob.model.ImageJobId;

import java.util.Optional;

public interface ImageJobPersistenceOutport {
    Optional<ImageJob> loadOne(ImageJobId id);
}
