package com.thoughtworks.fms.core.mybatis.filter;


import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@javax.ws.rs.ext.Provider
public class FileStreamFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LimitedInputStream limitedInputStream = new LimitedInputStream(requestContext.getEntityStream());
        requestContext.setEntityStream(limitedInputStream);
    }

    private static class LimitedInputStream extends FilterInputStream {
        private static final long MAX_SIZE = Long.valueOf(PropertiesLoader.getProperty("file.max.size"));
        private static final int EOF = -1;
        private long count;

        LimitedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        private void checkLimit() throws IOException {
            if (count > MAX_SIZE) {
                throw new InvalidRequestException(FMSErrorCode.FILE_SIZE_EXCEEDED);
            }
        }

        @Override
        public int read() throws IOException {
            int res = super.read();
            if (res != EOF) {
                count += res;
                checkLimit();
            }

            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res != EOF) {
                count += res;
                checkLimit();
            }

            return res;
        }

        @Override
        public void close() throws IOException {
            super.close();
        }

    }

}
