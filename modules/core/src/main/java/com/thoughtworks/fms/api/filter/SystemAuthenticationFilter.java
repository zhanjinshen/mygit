package com.thoughtworks.fms.api.filter;

import com.thoughtworks.fms.api.service.SystemAuthenticationService;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;
import java.util.Objects;

@javax.ws.rs.ext.Provider
public class SystemAuthenticationFilter implements ContainerRequestFilter {
    @Inject
    private ResourceInfo resourceInfo;

    @Inject
    private SystemAuthenticationService systemAuthenticationService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SystemAuthentication systemAuthentication = resourceInfo.getResourceMethod()
                .getAnnotation(SystemAuthentication.class);

        if (Objects.isNull(systemAuthentication)) {
            return;
        }

        systemAuthenticationService.ensureSystem(systemAuthentication.system(), requestContext);
    }
    
}


