package com.thoughtworks.fms.core;

import org.joda.time.DateTime;

public interface FileMetadata {

    long getId();

    String getSuffix();

    String getSourceName();

    String getDestName();

    long getSize();

    DateTime getCreatedAt();

    String getSwfFileName();

}
