package com.thoughtworks.fms.core.mybatis.exception;

import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;

import java.util.Properties;

public enum FMSErrorCode {

    SERVER_INTERNAL_ERROR("system.internal.error"),
    RESOURCE_NOT_FOUND("resource.not.found"),
    FILE_EXTENSION_NOT_ACCEPT("file.extension.not.accept"),
    FILE_SIZE_EXCEEDED("file.size.exceeded"),
    FILE_TOKEN_MISSING("file.token.missing"),
    FILES_ID_EMPTY("file.ids.empty"),
    FILE_TOKEN_INVALID("file.token.invalid"),
    UPLOAD_FILE_FAIL("upload.file.fail"),
    DOWNLOAD_FILE_FAIL("download.file.fail");

    private static Properties errorProperties = PropertiesLoader.loadProperties("error.properties");
    private String value;

    FMSErrorCode(String value) {
        this.value = value;
    }

    public String getMessageTemplate() {
        return errorProperties.getProperty(value, "");
    }

}
