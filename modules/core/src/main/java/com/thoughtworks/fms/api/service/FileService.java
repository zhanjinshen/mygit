package com.thoughtworks.fms.api.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface FileService {

    long store(String sourceName, String destName, InputStream inputStream);

    InputStream fetch(String destName);

    File fetch(List<Long> fileIds);

}
