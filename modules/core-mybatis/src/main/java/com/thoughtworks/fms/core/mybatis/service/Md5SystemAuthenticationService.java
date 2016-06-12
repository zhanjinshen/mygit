package com.thoughtworks.fms.core.mybatis.service;

import com.google.common.base.Strings;
import com.thoughtworks.fms.api.service.SystemAuthenticationService;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.util.Properties;

import static com.thoughtworks.fms.core.mybatis.util.Md5Util.getHmacMD5;
import static com.thoughtworks.fms.core.mybatis.util.PropertiesLoader.loadProperties;
import static java.lang.String.valueOf;

public class Md5SystemAuthenticationService implements SystemAuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(Md5SystemAuthenticationService.class);
    private static final Properties AUTH_PROPERTIES = loadProperties("fms.properties");

    @Override
    public void ensureSystem(String system, ContainerRequestContext requestContext) {
        final String midasMd5 = requestContext.getHeaderString("MIDAS-MD5");
        final String midasSystem = requestContext.getHeaderString("MIDAS-SYSTEM");
        final String timestamp = requestContext.getHeaderString("MIDAS-TIMESTAMP");

        if (Strings.isNullOrEmpty(midasSystem) || Strings.isNullOrEmpty(midasMd5)
                || Strings.isNullOrEmpty(timestamp)) {
            LOGGER.error("System Log: header MIDAS-SYSTEM/MIDAS-MD5/MIDAS-TIMESTAMP is missing");
            throw new ForbiddenException("header MIDAS-SYSTEM/MIDAS-MD5/MIDAS-TIMESTAMP is missing");
        }

        if (!midasSystem.equals(system)) {
            LOGGER.error("System Log: header MIDAS-SYSTEM is not matched");
            throw new ForbiddenException("header MIDAS-SYSTEM is not matched");
        }

        final String body;
        try {
            body = IOUtils.toString(requestContext.getEntityStream(), "UTF-8");
        } catch (IOException e) {
            LOGGER.error("System Log: failed to get string of entity stream");
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        final String expectedMidasMd5;
        try {
            expectedMidasMd5 = getHmacMD5(AUTH_PROPERTIES.getProperty(system + ".secret"),
                    body.concat(valueOf(Long.valueOf(timestamp))));
        } catch (Exception e) {
            LOGGER.error("System Log: handle MD5 error", e);
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        if (!midasMd5.equals(expectedMidasMd5)) {
            LOGGER.error("System Log: MD5 not matched");
            throw new ForbiddenException("MD5 not matched");
        }

        requestContext.setEntityStream(IOUtils.toInputStream(body));
    }

}
