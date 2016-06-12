package com.thoughtworks.fms.core.mybatis.exception;

public class InvalidRequestException extends FMSRuntimeException {

    public InvalidRequestException(FMSErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

}
