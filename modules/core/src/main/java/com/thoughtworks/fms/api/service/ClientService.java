package com.thoughtworks.fms.api.service;

public interface ClientService {

    void informUms(String uri, Long fileId, String fileName);

    void informCredit(String uri, Long fileId, String fileName, String destName);

    void informCreditBigFile(String uri, Long fileId, String fileName, String destName,String source);

}
