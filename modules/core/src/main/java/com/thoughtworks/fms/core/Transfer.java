package com.thoughtworks.fms.core;

import com.thoughtworks.fms.exception.TransferException;

import java.io.InputStream;

public interface Transfer {

    void write(String name, InputStream inputStream) throws TransferException;

    InputStream read(String name) throws TransferException;

}
