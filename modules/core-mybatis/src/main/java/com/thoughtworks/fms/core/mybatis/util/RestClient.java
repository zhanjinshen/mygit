package com.thoughtworks.fms.core.mybatis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InternalServerException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

import static java.lang.String.valueOf;

public class RestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
    private static final String UMS_AUTH_SECRET = PropertiesLoader.getProperty("fms.secret");

    public <T> T post(String url, Object entity, Function<Response, T> handler) {
        Client client = ClientBuilder.newClient();
        Response response = null;
        try {
            long timestamp = new Date().getTime();
            response = client.target(url)
                    .request()
                    .header("MIDAS-TIMESTAMP", timestamp)
                    .header("MIDAS-SYSTEM", "fms")
                    .header("MIDAS-MD5", getMd5(entity, timestamp))
                    .post(Entity.json(entity));
            return handler.apply(response);
        } finally {
            closeConnection(response);
            closeClient(client);
        }
    }

    public <T> T get(String url, String system, String secret, Function<Response, T> handler) {
        Client client = ClientBuilder.newClient();
        Response response = null;
        try {
            long timestamp = new Date().getTime();
            response = client.target(url)
                    .request()
                    .header("MIDAS-TIMESTAMP", timestamp)
                    .header("MIDAS-SYSTEM", system)
                    .header("MIDAS-MD5", getMd5("", secret, timestamp))
                    .get();
            return handler.apply(response);
        } finally {
            closeConnection(response);
            closeClient(client);
        }
    }

    private void closeClient(Client client) {
        try {
            if (Objects.nonNull(client)) {
                client.close();
            }
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

    private void closeConnection(Response response) {
        try {
            if (Objects.nonNull(response)) {
                response.close();
            }
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

    private String getMd5(Object entity, long timestamp) {
        return getMd5(entity, UMS_AUTH_SECRET, timestamp);
    }

    private String getMd5(Object entity, String secret, long timestamp) {
        try {
            String entityStr = "";
            if (!"".equals(entity)) {
                entityStr = entityString(entity);
            }

            return Md5Util.getHmacMD5(secret, entityStr.concat(valueOf(timestamp)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

    private String entityString(Object entity) {
        final ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

}

