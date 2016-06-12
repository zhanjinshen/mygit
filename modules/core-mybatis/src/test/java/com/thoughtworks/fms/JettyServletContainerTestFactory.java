package com.thoughtworks.fms;

import com.google.inject.servlet.GuiceFilter;
import com.thoughtworks.midas.session.SessionFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.EnumSet;

public class JettyServletContainerTestFactory implements TestContainerFactory {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(JettyServletContainerTestFactory.class.getName());

    @Override
    public TestContainer create(URI baseUri, DeploymentContext deploymentContext) {
        return new JettyTestContainer(URI.create("http://127.0.0.1:9999"));
    }

    private static class JettyTestContainer implements TestContainer {
        private static final Logger logger = LoggerFactory.getLogger(JettyTestContainer.class.getName());
        private final URI baseUri;
        private HttpServer server;

        private JettyTestContainer(URI baseUri) {
            this.baseUri = UriBuilder.fromUri(baseUri).build();
        }

        @Override
        public ClientConfig getClientConfig() {
            ClientConfig clientConfig = new ClientConfig();
            return clientConfig.register(new LoggingFilter(LOGGER, true));
        }

        @Override
        public URI getBaseUri() {
            return baseUri;
        }

        @Override
        public void start() {
            logger.info("Starting JettyTestContainer...");
            try {
                System.setProperty("env", "mysql");

                final WebappContext context = new WebappContext("FMS TEST", "/");
                ServletRegistration servletRegistration = context.addServlet("ServletContainer", ServletContainer.class);
                servletRegistration.addMapping("/*");

                FilterRegistration sessionFilter = context.addFilter("sessionFilter", SessionFilter.class);
                sessionFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");

                servletRegistration.setInitParameter("javax.ws.rs.Application", "com.thoughtworks.fms.TestResourceConfig");
                FilterRegistration registration = context.addFilter("GuiceFilter", GuiceFilter.class);
                registration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), "/*");

                this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri, false);
                context.deploy(server);

                this.server.start();
            } catch (Exception e) {
                throw new TestContainerException(e);
            }
        }

        @Override
        public void stop() {
            logger.info("Stopping JettyTestContainer...");
            try {
                this.server.shutdownNow();
            } catch (Exception ex) {
                logger.info("Error Stopping JettyTestContainer...", ex);
            }
        }
    }
}