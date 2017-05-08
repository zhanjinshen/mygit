package com.thoughtworks.fms.core;

import java.util.List;

public interface FileRepository {

    long storeMetadata(String sourceName, String destName, String suffix, long size);

    long storeMetadataForCredit(String sourceName, String destName, String suffix, long size,String swfName);

    FileMetadata findMetadataById(long fileId);

    List<FileMetadata> findMetadataByIds(List<Long> fileIds);

}
