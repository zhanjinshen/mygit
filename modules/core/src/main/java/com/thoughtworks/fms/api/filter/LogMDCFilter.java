package com.thoughtworks.fms.api.filter;

import org.slf4j.MDC;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Random;

@Provider
@PreMatching
public class LogMDCFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_UID = "R_UID";
    private static final Random RANDOM = new Random();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MDC.put(REQUEST_UID, getRUID(8));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.clear();
    }

    private String getRUID(int len) {
        StringBuffer uid = new StringBuffer();
        for (int i = 0; i < len; i++) {
            int rand = RANDOM.nextInt(1000);
            int mod36 = rand % 36;
            encodeAndAdd(uid, mod36);
        }
        return uid.toString();
    }

    private void encodeAndAdd(StringBuffer ret, long mod36Val) {
        if (mod36Val < 10) {
            ret.append((char) (((int) '0') + (int) mod36Val));
        } else {
            ret.append((char) (((int) 'a') + (int) (mod36Val - 10)));
        }
    }

}
