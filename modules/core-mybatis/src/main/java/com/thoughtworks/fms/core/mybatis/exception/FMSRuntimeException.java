package com.thoughtworks.fms.core.mybatis.exception;

import java.text.MessageFormat;

public class FMSRuntimeException extends RuntimeException {

    private final FMSErrorCode code;
    private final Object[] args;

    public FMSRuntimeException(FMSErrorCode errorCode, Object... args) {
        super(new MessageFormat(errorCode.getMessageTemplate()).format(args));
        this.code = errorCode;
        this.args = args;
    }

    public FMSRuntimeException(FMSErrorCode errorCode, Throwable cause, Object... args) {
        super(new MessageFormat(errorCode.getMessageTemplate()).format(args), cause);
        this.code = errorCode;
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public FMSErrorCode getCode() {
        return code;
    }

}
