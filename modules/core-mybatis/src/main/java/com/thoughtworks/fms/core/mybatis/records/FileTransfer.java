package com.thoughtworks.fms.core.mybatis.records;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.exception.TransferException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class FileTransfer implements Transfer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransfer.class);

    private static String END_POINT = PropertiesLoader.getProperty("oss.end.point");
    private static String BUCKET_NAME = PropertiesLoader.getProperty("oss.bucket.name");
    private static String CREDIT_BUCKET_NAME = PropertiesLoader.getProperty("oss.credit.bucket.name");
    private static String ACCESS_KEY_ID = PropertiesLoader.getProperty("oss.access.key.id");
    private static String ACCESS_KEY_SECRET = PropertiesLoader.getProperty("oss.access.key.secret");

    private static OSSClient client;

    static {
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(20 * 1000);
        conf.setSocketTimeout(20 * 1000);

        client = new OSSClient(END_POINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET, conf);
    }

    @Override
    public void write(String name, InputStream inputStream) throws TransferException {
        try {
            client.putObject(new PutObjectRequest(BUCKET_NAME, name, inputStream));
            LOGGER.info("System Log: upload " + name + " file successful.");
        } catch (ClientException | OSSException e) {
            throw new TransferException(e.getMessage(), e);
        }
    }

    @Override
    public void writeForCredit(String name, InputStream inputStream) throws TransferException {
        try {
            client.putObject(new PutObjectRequest(CREDIT_BUCKET_NAME, name, inputStream));
            LOGGER.info("System Log: upload " + name + " file successful.");
        } catch (ClientException | OSSException e) {
            throw new TransferException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream read(String name) throws TransferException {
        OSSObject object;
        try {
            object = client.getObject(new GetObjectRequest(BUCKET_NAME, name));
            LOGGER.info("System Log: download " + name + " file successful.");
        } catch (ClientException | OSSException e) {
            throw new TransferException(e.getMessage(), e);
        }

        return object.getObjectContent();
    }

    @Override
    public InputStream readForCredit(String name) throws TransferException {
        OSSObject object;
        try {
            object = client.getObject(new GetObjectRequest(CREDIT_BUCKET_NAME, name));
            LOGGER.info("System Log: download " + name + " file successful.");
        } catch (ClientException | OSSException e) {
            throw new TransferException(e.getMessage(), e);
        }

        return object.getObjectContent();
    }

}
