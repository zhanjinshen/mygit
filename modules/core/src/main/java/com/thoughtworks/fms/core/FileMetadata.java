package com.thoughtworks.fms.core;

import org.joda.time.DateTime;

public interface FileMetadata {

    long getId();

    String getSuffix();

    String getName();

    long getSize();

    DateTime getCreatedAt();

}
