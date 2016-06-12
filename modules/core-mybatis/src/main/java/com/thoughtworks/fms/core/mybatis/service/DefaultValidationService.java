package com.thoughtworks.fms.core.mybatis.service;

import com.google.common.base.Strings;
import com.thoughtworks.fms.api.resources.ServerProperties;
import com.thoughtworks.fms.api.service.ValidationService;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

public class DefaultValidationService implements ValidationService {

    @Inject
    private HttpServletRequest request;

    @Inject
    private ServerProperties properties;

    @Override
    public void ensureUploadTokenValid(FormDataBodyPart tokenField) {
        if (Objects.isNull(tokenField)) {
            throw new InvalidRequestException(FMSErrorCode.FILE_TOKEN_MISSING);
        }

        ensureTokenValid(properties.getUploadTokenKey(tokenField.getValueAs(String.class)));
    }

    @Override
    public void ensureDownloadTokenValid(String token) {
        if (Strings.isNullOrEmpty(token)) {
            throw new InvalidRequestException(FMSErrorCode.FILE_TOKEN_MISSING);
        }

        ensureTokenValid(properties.getDownloadTokenKey(token));
    }

    private void ensureTokenValid(String key) {
        Object obj = request.getSession().getAttribute(key);
        if (Objects.isNull(obj)) {
            throw new InvalidRequestException(FMSErrorCode.FILE_TOKEN_INVALID);
        }
    }

}
