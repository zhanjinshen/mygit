package com.thoughtworks.fms.core;

import java.util.List;

public interface FileRepository {

    long storeMetadata(String sourceName, String destName, String suffix, long size);

    FileMetadata findMetadataById(long fileId);

    List<FileMetadata> findMetadataByIds(List<Long> fileIds);

}
