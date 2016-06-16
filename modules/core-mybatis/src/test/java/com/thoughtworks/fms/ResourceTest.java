package com.thoughtworks.fms;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Before;
import org.mockito.Mockito;

import javax.ws.rs.core.Application;

import static org.glassfish.jersey.server.ResourceConfig.forApplicationClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

public class ResourceTest extends JerseyTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Mockito.reset(TestResourceConfig.transfer,
                TestResourceConfig.sessionService,
                TestResourceConfig.validationService,
                TestResourceConfig.clientService);

        doNothing().when(TestResourceConfig.transfer).write(any(), any());
        doNothing().when(TestResourceConfig.sessionService).removeAttribute(any(), any());
        doNothing().when(TestResourceConfig.clientService).informUms(any(), any(), any());
    }

    @Override
    protected Application configure() {
        return forApplicationClass(TestResourceConfig.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new JettyServletContainerTestFactory();
    }

}
