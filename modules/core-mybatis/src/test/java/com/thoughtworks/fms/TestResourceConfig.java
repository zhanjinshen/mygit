package com.thoughtworks.fms;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.thoughtworks.fms.api.resources.ServerProperties;
import com.thoughtworks.fms.api.service.*;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.FileRepository;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.app.ModelModule;
import com.thoughtworks.fms.core.mybatis.records.FileCipher;
import com.thoughtworks.fms.core.mybatis.records.FileTransfer;
import com.thoughtworks.fms.core.mybatis.service.DefaultFileService;
import com.thoughtworks.fms.core.mybatis.service.Md5SystemAuthenticationService;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.mockito.Mockito;

import javax.inject.Inject;

import static java.util.Arrays.asList;

public class TestResourceConfig extends ResourceConfig {
    public static ValidationService validationService;
    public static SessionService sessionService;
    public static Transfer transfer;
    public static ClientService clientService;

    static {
        validationService = Mockito.mock(ValidationService.class);
        clientService = Mockito.mock(ClientService.class);
        sessionService = Mockito.mock(SessionService.class);
        transfer = Mockito.mock(FileTransfer.class);
    }

    @Inject
    public TestResourceConfig(ServiceLocator serviceLocator) throws Exception {
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);

        Injector injector = Guice.createInjector(asList(new ModelModule("env"), mockModule()));
        property(org.glassfish.jersey.server.ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);

        packages("com.thoughtworks.fms");
        register(MultiPartFeature.class);
        guiceBridge.bridgeGuiceInjector(injector);
    }

    static AbstractModule mockModule() {
        ServerProperties properties = new ServerProperties(PropertiesLoader.loadProperties("fms.properties"));
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(ServerProperties.class).toInstance(properties);
                bind(Transfer.class).toInstance(transfer);
                bind(Cipher.class).to(FileCipher.class);
                bind(ClientService.class).toInstance(clientService);
                bind(ValidationService.class).toInstance(validationService);
                bind(SessionService.class).toInstance(sessionService);
                bind(FileService.class).to(DefaultFileService.class);
                bind(FileRepository.class).to(com.thoughtworks.fms.core.mybatis.records.FileRepository.class);
                bind(SystemAuthenticationService.class).to(Md5SystemAuthenticationService.class);
            }
        };
    }

}
