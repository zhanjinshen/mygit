package com.thoughtworks.fms.core.mybatis.exception;

public class EntityNotFoundException extends FMSRuntimeException {

    public EntityNotFoundException(FMSErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public EntityNotFoundException(FMSErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

}
