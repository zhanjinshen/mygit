package com.thoughtworks.fms.core.mybatis.mappers;

import com.thoughtworks.fms.core.FileMetadata;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FileMetadataMapper {

    void createMetadata(@Param("suffix") String suffix,
                        @Param("name") String name,
                        @Param("fileSize") long size,
                        @Param("piggyback") Map<String, Long> piggyback);

    FileMetadata findMetadataById(@Param("id") long id);

    List<FileMetadata> findMetadataByIds(@Param("ids") List<Long> fileIds);

}
