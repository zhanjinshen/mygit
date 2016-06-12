package com.thoughtworks.fms.core.mybatis.exception;

public class NotFoundException extends FMSRuntimeException {

    public NotFoundException(FMSErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

}
