package com.thoughtworks.fms.core.mybatis.records;

import org.joda.time.DateTime;

public class FileMetadata implements com.thoughtworks.fms.core.FileMetadata {

    private long id;
    private String suffix;
    private String sourceName;
    private String destName;
    private long size;
    private DateTime createdAt;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public String getDestName() {
        return destName;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public DateTime getCreatedAt() {
        return createdAt;
    }

}
