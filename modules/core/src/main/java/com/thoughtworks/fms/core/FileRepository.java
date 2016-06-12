package com.thoughtworks.fms.core;

import java.util.List;

public interface FileRepository {

    long storeMetadata(String suffix, String name, long size);

    FileMetadata findMetadataById(long fileId);

    List<FileMetadata> findMetadataByIds(List<Long> fileIds);

}
