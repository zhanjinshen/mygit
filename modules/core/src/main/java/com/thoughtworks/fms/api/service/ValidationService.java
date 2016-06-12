package com.thoughtworks.fms.api.service;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

public interface ValidationService {

    void ensureUploadTokenValid(FormDataBodyPart tokenField);

    void ensureDownloadTokenValid(String token);

}
