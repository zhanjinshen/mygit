package com.thoughtworks.fms.api.resources;

import java.util.Properties;

public class ServerProperties {

    private Properties properties;

    public ServerProperties(Properties properties) {
        this.properties = properties;
    }

    public String getUploadTokenKey(String token) {
        return (new StringBuffer()).append(properties.getProperty("file.upload.token.key", "UPLOAD-TOKEN-"))
                .append(token).toString();
    }

    public String getDownloadTokenKey(String token) {
        return (new StringBuffer()).append(properties.getProperty("file.upload.token.key", "DOWNLOAD-TOKEN-"))
                .append(token).toString();
    }

}
