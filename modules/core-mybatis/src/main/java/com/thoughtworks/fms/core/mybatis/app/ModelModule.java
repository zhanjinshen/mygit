package com.thoughtworks.fms.core.mybatis.app;

import com.google.inject.AbstractModule;

public class ModelModule extends AbstractModule {
    private final String environment;

    public ModelModule(String environment) {
        this.environment = environment;
    }

    @Override
    protected void configure() {
        install(new PersistenceModule(environment));
    }

}
