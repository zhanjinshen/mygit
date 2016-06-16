package com.thoughtworks.fms.core.mybatis.service;

import com.thoughtworks.fms.api.service.ClientService;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InternalServerException;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.core.mybatis.util.RestClient;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class DefaultClientService implements ClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClientService.class);
    private static final String UMS_URI = PropertiesLoader.getProperty("ums.url");
    private static final RestClient CLIENT = new RestClient();

    @Override
    public void informUms(String uri, Long fileId, String fileName) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("fileId", fileId);
        entity.put("fileName", fileName);

        LOGGER.debug("System Log: The callback url of ums is: " + UMS_URI + uri);

        CLIENT.put(UMS_URI + uri, entity, (Response response) -> {
            if (response.getStatus() != HttpStatus.NO_CONTENT_204.getStatusCode()) {
                LOGGER.error("System Log: Error callback ums with status:{} detail message: {}",
                        response.getStatus(), response);
                throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
            }
            return null;
        });
    }

}
