package com.thoughtworks.fms.core.mybatis.filter;

import com.google.common.base.Strings;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@javax.ws.rs.ext.Provider
@PreMatching
public class CsrfProtectionFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRequestFilter.class);
    private final static Properties properties = PropertiesLoader.loadProperties("fms.properties");
    private final static String CSRF_HEADER_NAME = "CSRF-TOKEN";
    private final static String CSRF_COOKIE_NAME = "_csrf";

    @Context
    private HttpServletResponse response;

    public static String buildCookieHeader(NewCookie newCookie) {
        String cookieHeader = MessageFormat.format("{0}={1}; Path={2};", newCookie.getName(), newCookie.getValue(), newCookie.getPath());
        if (newCookie.getExpiry() != null) {
            cookieHeader = cookieHeader.concat("Expires=" + formatExpire(newCookie.getExpiry()) + ";");
        }
        if (newCookie.isHttpOnly()) {
            cookieHeader = cookieHeader.concat("HttpOnly;");
        }
        if (newCookie.isSecure()) {
            cookieHeader = cookieHeader.concat("Secure;");
        }
        return cookieHeader;
    }

    private static String formatExpire(Date date) {
        final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(date);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String url = requestContext.getUriInfo().getAbsolutePath().toASCIIString();
        String userAgent = requestContext.getHeaderString("user-agent");
        Cookie cookie = requestContext.getCookies().get(CSRF_COOKIE_NAME);
        String headerValue = requestContext.getHeaderString(CSRF_HEADER_NAME);
        String method = requestContext.getMethod();

        if (method.equals("GET")) {
            if (Objects.isNull(cookie)) {
                LOGGER.debug("System Log: set csrf cookie");
                response.addHeader("SET-COOKIE", buildCookieHeader(generateCsrfCookie(CSRF_COOKIE_NAME)));
            }

            return;
        }

        // IE 8,9 compatible issue
        if (method.equals("POST") && (userAgent.contains("MSIE 9.0") ||userAgent.contains("MSIE 8.0"))
                && url.endsWith("files")){
            return;
        }

        String headerString = requestContext.getHeaderString("MIDAS-SYSTEM");
        if (!Strings.isNullOrEmpty(headerString)) {
            return;
        }

        if (cookie == null || cookie.getValue() == null
                || headerValue == null
                || !cookie.getValue().equals(headerValue)) {

            LOGGER.debug("System Log: user refused to login because of no CSRF header");

            Response.ResponseBuilder badRequestBuilder = Response.status(400);
            response.getHeaderNames().stream().forEach(headerName -> badRequestBuilder.header(headerName, response.getHeader(headerName)));

            badRequestBuilder.header("SET-COOKIE", buildCookieHeader(generateCsrfCookie(CSRF_COOKIE_NAME))).build();
            requestContext.abortWith(badRequestBuilder.entity(csrfBadRequestInfo()).build());
        }
    }

    private Map<String, String> csrfBadRequestInfo() {
        Map<String, String> detail = new HashMap<>();
        detail.put("code", "CSRF_NOT_PROVIDED");
        detail.put("message", "CSRF header or cookie is not provided");
        return detail;
    }

    private NewCookie generateCsrfCookie(String csrfCookieName) {
        return new NewCookie(csrfCookieName, UUID.randomUUID().toString(), "/", "", "", -1,
                Boolean.valueOf(properties.getProperty("csrf.cookie.secure", "false")));
    }
}
