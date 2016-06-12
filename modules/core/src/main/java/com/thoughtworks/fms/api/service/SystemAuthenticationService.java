package com.thoughtworks.fms.api.service;

import javax.ws.rs.container.ContainerRequestContext;

public interface SystemAuthenticationService {
    void ensureSystem(String system, ContainerRequestContext requestContext);
}
