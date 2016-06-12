package com.thoughtworks.fms.core.mybatis.filter;


import org.apache.ibatis.session.SqlSessionManager;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

@javax.ws.rs.ext.Provider
@PreMatching
public class OpenSessionInViewRequestFilter implements ContainerRequestFilter {
    @Inject
    private SqlSessionManager sqlSessionManager;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!sqlSessionManager.isManagedSessionStarted()) {
            sqlSessionManager.startManagedSession();
        }
    }

}
