package com.thoughtworks.fms.api.service;

public interface ClientService {

    void informUms(String uri, Long fileId, String fileName);

    void informCredit(String uri, Long fileId, String fileName);

}
