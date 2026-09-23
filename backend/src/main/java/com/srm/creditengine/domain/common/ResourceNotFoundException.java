package com.srm.creditengine.domain.common;

/** The requested resource does not exist. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s com identificador %s não existe.".formatted(resource, id));
    }
}
