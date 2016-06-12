package com.thoughtworks.fms.core.mybatis.app;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.thoughtworks.fms.api.resources.ServerProperties;
import com.thoughtworks.fms.api.service.*;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.FileRepository;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.records.FileCipher;
import com.thoughtworks.fms.core.mybatis.records.FileTransfer;
import com.thoughtworks.fms.core.mybatis.service.DefaultClientService;
import com.thoughtworks.fms.core.mybatis.service.DefaultFileService;
import com.thoughtworks.fms.core.mybatis.service.DefaultValidationService;
import com.thoughtworks.fms.core.mybatis.service.Md5SystemAuthenticationService;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import javax.inject.Inject;

public class FMSResourceConfig extends ResourceConfig {

    @Inject
    public FMSResourceConfig(ServiceLocator serviceLocator) throws Exception {
        ServerProperties properties = new ServerProperties(PropertiesLoader.loadProperties("fms.properties"));

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        Injector injector = Guice.createInjector(
                new ModelModule("env"),
                new AbstractModule() {
                    @Override
                    public void configure() {
                        bind(Transfer.class).to(FileTransfer.class);
                        bind(Cipher.class).to(FileCipher.class);
                        bind(ServerProperties.class).toInstance(properties);
                        bind(SessionService.class).toInstance(new SessionService());
                        bind(FileRepository.class).to(com.thoughtworks.fms.core.mybatis.records.FileRepository.class);
                        bind(FileService.class).to(DefaultFileService.class);
                        bind(ClientService.class).to(DefaultClientService.class);
                        bind(SystemAuthenticationService.class).to(Md5SystemAuthenticationService.class);
                    }
                }
        );

        property(org.glassfish.jersey.server.ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);

        packages("com.thoughtworks.fms");
        register(MultiPartFeature.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(DefaultValidationService.class).to(ValidationService.class);
            }
        });

        guiceBridge.bridgeGuiceInjector(injector);
    }

}
