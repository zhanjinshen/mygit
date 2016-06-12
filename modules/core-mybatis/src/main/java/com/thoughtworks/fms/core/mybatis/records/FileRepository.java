package com.thoughtworks.fms.core.mybatis.records;

import com.thoughtworks.fms.core.FileMetadata;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import com.thoughtworks.fms.core.mybatis.exception.NotFoundException;
import com.thoughtworks.fms.core.mybatis.mappers.FileMetadataMapper;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileRepository implements com.thoughtworks.fms.core.FileRepository {

    @Inject
    private FileMetadataMapper metadataMapper;

    @Override
    public long storeMetadata(String suffix, String name, long size) {
        Map<String, Long> piggyback = new HashMap<>();
        metadataMapper.createMetadata(suffix, name, size, piggyback);
        return piggyback.get("id");
    }

    @Override
    public FileMetadata findMetadataById(long fileId) {
        FileMetadata metadata = metadataMapper.findMetadataById(fileId);

        if (Objects.isNull(metadata)) {
            throw new NotFoundException(FMSErrorCode.RESOURCE_NOT_FOUND, fileId);
        }

        return metadata;
    }

    @Override
    public List<FileMetadata> findMetadataByIds(List<Long> fileIds) {
        if (Objects.isNull(fileIds) || fileIds.isEmpty()) {
            throw new InvalidRequestException(FMSErrorCode.FILES_ID_EMPTY);
        }

        return metadataMapper.findMetadataByIds(fileIds);
    }

}
