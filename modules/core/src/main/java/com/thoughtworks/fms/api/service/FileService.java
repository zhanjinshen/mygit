package com.thoughtworks.fms.api.service;

import com.thoughtworks.fms.core.FileMetadata;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileService {

    long store(String sourceName, String destName, InputStream inputStream);

    InputStream fetch(String destName);

    File fetch(List<Long> fileIds, String zipFileName);
    
    String getUrl(String key);

    long storeForCredit(String sourceName, String destName, InputStream inputStream,String source,String swfName);

    File fetchForCredit(List<Long> fileIds, String zipFileName);

    InputStream fetchForCredit(String destName);

    public  String convertForView(File sourceFile);

    public  Map doc2swf(String fileString) throws Exception;

    public   void runOpenOffice() throws Exception;

    public  String saveUploadFileForView(InputStream inputStreamFile,String destName);

    public FileMetadata findMetadataById(long fileId);
}
