package com.thoughtworks.fms.core.mybatis.exception;

public class InternalServerException extends FMSRuntimeException {
    public InternalServerException(FMSErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
