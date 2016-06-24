package com.thoughtworks.fms.core.mybatis.exception.mappers;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    public Response toResponse(Exception exception) {
        LOGGER.error(ExceptionUtils.getFullStackTrace(exception));

        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        } else {
            final Map<String, Object> error = new HashMap<>();
            error.put("code", "SERVER_ERROR");
            error.put("message", "请稍后重试");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)
                    .type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }
}
