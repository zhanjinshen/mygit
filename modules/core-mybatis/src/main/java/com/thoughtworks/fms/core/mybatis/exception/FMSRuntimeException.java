package com.thoughtworks.fms.core.mybatis.exception;

import java.text.MessageFormat;

public class FMSRuntimeException extends RuntimeException {

    private final FMSErrorCode code;

    public FMSRuntimeException(FMSErrorCode errorCode, Object... args) {
        super(new MessageFormat(errorCode.getMessageTemplate() == null ? "" : errorCode.getMessageTemplate()).format(args));
        this.code = errorCode;
    }

    public FMSErrorCode getCode() {
        return code;
    }

}
