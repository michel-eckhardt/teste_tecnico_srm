package com.srm.creditengine.domain.common;

/** The requested resource does not exist. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        this("%s com identificador %s não existe.".formatted(resource, id));
    }

    public ResourceNotFoundException(String detail) {
        super(ErrorCode.RESOURCE_NOT_FOUND, detail);
    }
}
