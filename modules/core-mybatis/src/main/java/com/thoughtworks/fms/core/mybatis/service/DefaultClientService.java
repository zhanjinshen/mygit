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
    private static final String CREDIT_URI = PropertiesLoader.getProperty("credit.url");
    private static final RestClient CLIENT = new RestClient();

    @Override
    public void informUms(String uri, Long fileId, String fileName) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("fileId", fileId);
        entity.put("fileName", fileName);

        LOGGER.debug("System Log: The callback url of ums is: " + UMS_URI + uri);

        CLIENT.post(UMS_URI + uri, entity, (Response response) -> {
            if (response.getStatus() != HttpStatus.NO_CONTENT_204.getStatusCode()) {
                LOGGER.error("System Log: Error callback ums with status:{} detail message: {}",
                        response.getStatus(), response);
                throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
            }
            return null;
        });
    }

    /**
     * 将上传好的id返回给credit
     * @param uri
     * @param fileId
     * @param fileName
     */
    @Override
    public void informCredit(String uri, Long fileId, String fileName, String destName) {
        //将返回的id存入credit项目
        Map<String, Object> entity = new HashMap<>();
        entity.put("fileId", fileId);
        entity.put("fileName", fileName);
        entity.put("url", destName);
        entity.put("name", fileName.split("\\.")[0]);
        LOGGER.debug("System Log: The callback url of credit is: " + CREDIT_URI + uri);
        CLIENT.postForCredit(CREDIT_URI + uri, entity, (Response response) -> {
//            if (response.getStatus() != HttpStatus.NO_CONTENT_204.getStatusCode()) {
//                LOGGER.error("System Log: Error callback credit with status:{} detail message: {}",
//                        response.getStatus(), response);
//                throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
//            }
            return null;
        });
    }

    @Override
    public void informCreditBigFile(String uri, Long fileId, String fileName, String destName, String creditSource, String sourceId) {
        //将返回的id存入credit项目
        Map<String, Object> entity = new HashMap<>();
        entity.put("fileId", fileId);
        entity.put("fileName", fileName);
        entity.put("url", destName);
        entity.put("creditSource", creditSource);
        entity.put("name", fileName.split("\\.")[0]);
        entity.put("sourceId", sourceId);
        LOGGER.debug("System Log: The callback url of credit is: " + CREDIT_URI + uri);
        LOGGER.info("fileId：" + fileId + " fileName："+fileName+ " destName："+destName+" creditSource："+creditSource+" sourceId："+sourceId);
        CLIENT.postForCreditBigFile(CREDIT_URI + uri, entity, (Response response) -> {
//            if (response.getStatus() != HttpStatus.NO_CONTENT_204.getStatusCode()) {
//                LOGGER.error("System Log: Error callback credit with status:{} detail message: {}",
//                        response.getStatus(), response);
//                throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
//            }
            return null;
        });
    }

    @Override
    public void completeCreditAttachmentForBigFile(String uri, String sourceId, int totalFileNum) {
        LOGGER.info("uri：" + CREDIT_URI + uri + " sourceId：" + sourceId + " totalFileNum：" + totalFileNum);
        CLIENT.postForCompleteCreditBigFile(CREDIT_URI + uri, sourceId, totalFileNum, (Response response) -> {
            return null;
        });
    }


}
