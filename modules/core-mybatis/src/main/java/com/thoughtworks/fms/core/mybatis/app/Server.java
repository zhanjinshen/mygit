package com.thoughtworks.fms.core.mybatis.app;

import com.google.inject.servlet.GuiceFilter;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.midas.session.SessionFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.DispatcherType;
import java.net.URI;
import java.util.EnumSet;
import java.util.Properties;

public class Server {

    private static URI BASE_URI = URI.create("http://0.0.0.0:8086/");

    public static void main(String[] args) throws Exception {
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, false);
        final WebappContext context = new WebappContext("UMS API Server", "/fms");
        context.setAttribute("contextPath", "/fms");

        ServletRegistration servletRegistration = context.addServlet("ServletContainer", ServletContainer.class);

        servletRegistration.addMapping("/*");
        servletRegistration.setInitParameter("javax.ws.rs.Application", "com.thoughtworks.fms.core.mybatis.app.FMSResourceConfig");

        FilterRegistration sessionFilter = context.addFilter("sessionFilter", SessionFilter.class);
        Properties properties = PropertiesLoader.loadProperties("fms.properties");
        sessionFilter.setInitParameter("periodSeconds", properties.getProperty("login.period", "1800"));
        sessionFilter.setInitParameter("cookiePath", properties.getProperty("p2p.session.key.path", "/"));
        sessionFilter.setInitParameter("httpOnly", properties.getProperty("p2p.session.key.httponly", "true"));
        sessionFilter.setInitParameter("secure", properties.getProperty("p2p.session.key.secure", "false"));
        sessionFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");

        FilterRegistration registration = context.addFilter("GuiceFilter", GuiceFilter.class);
        registration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");

        context.deploy(server);

        server.start();
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                server.shutdownNow();
            }
        }
    }
}
