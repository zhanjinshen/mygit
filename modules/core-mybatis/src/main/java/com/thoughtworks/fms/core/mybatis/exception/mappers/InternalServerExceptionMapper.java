package com.thoughtworks.fms.core.mybatis.exception.mappers;

import com.thoughtworks.fms.core.mybatis.exception.InternalServerException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class InternalServerExceptionMapper implements ExceptionMapper<InternalServerException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalServerExceptionMapper.class);

    @Override
    public Response toResponse(InternalServerException exception) {
        LOGGER.error(ExceptionUtils.getFullStackTrace(exception));

        final Map<String, String> exceptionMap = new HashMap<>();
        exceptionMap.put("code", exception.getCode().toString());
        exceptionMap.put("message", exception.getMessage() == null ? "" : exception.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exceptionMap)
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}
