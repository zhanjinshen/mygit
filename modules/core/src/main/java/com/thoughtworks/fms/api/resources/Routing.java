package com.thoughtworks.fms.api.resources;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class Routing {

    private static URI template(String pattern, Object... args) {
        return UriBuilder.fromUri(pattern).build(args);
    }

    public static URI file(long fileId) {
        return template("/files/{file_id}", fileId);
    }

}
