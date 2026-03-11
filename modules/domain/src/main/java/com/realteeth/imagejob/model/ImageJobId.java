package com.realteeth.imagejob.model;

import com.realteeth.error.exception.DomainException;
import com.realteeth.error.info.ImageJobErrorInfo;
import lombok.Getter;

@Getter
public class ImageJobId {
    private final long value;

    public ImageJobId(Long input) {
        validate(input);
        this.value = input;
    }

    private void validate(Long input) {
        if (input == null)
            throw new DomainException(ImageJobErrorInfo.ID_NOT_EXIST);
        if (input <= 0) {
            throw new DomainException(ImageJobErrorInfo.ID_NOT_POSITIVE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImageJobId other)) {
            return false;
        }
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }
}
