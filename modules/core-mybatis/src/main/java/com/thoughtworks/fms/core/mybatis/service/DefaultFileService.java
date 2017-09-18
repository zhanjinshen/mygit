package com.thoughtworks.fms.core.mybatis.service;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.google.common.base.Splitter;
import com.thoughtworks.fms.api.service.FileService;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.FileMetadata;
import com.thoughtworks.fms.core.FileRepository;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InternalServerException;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import com.thoughtworks.fms.core.mybatis.util.ConvertUtil;
import com.thoughtworks.fms.core.mybatis.util.DateTimeHelper;
import com.thoughtworks.fms.core.mybatis.util.FileBuilder;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;
import com.thoughtworks.fms.exception.TransferException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;

    public class DefaultFileService implements FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFileService.class);

    private static final String FILE_SERVERS = PropertiesLoader.getProperty("file.servers");




    private static final int BUFFER = 2048;
    private static final int EOF = -1;
    private static List<String> ACCEPT_EXTENSIONS = Splitter.on(",")
            .splitToList(PropertiesLoader.getProperty("file.accept.extensions"));
    
   
    private static String END_POINT = PropertiesLoader.getProperty("oss.end.point");
    private static String BUCKET_NAME = PropertiesLoader.getProperty("oss.bucket.name");
    private static String ACCESS_KEY_ID = PropertiesLoader.getProperty("oss.access.key.id");
    private static String ACCESS_KEY_SECRET = PropertiesLoader.getProperty("oss.access.key.secret");

    private static OSSClient client;

    static {
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(20 * 1000);
        conf.setSocketTimeout(20 * 1000);

        client = new OSSClient(END_POINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET, conf);
    }

    @Inject
    private FileRepository repository;

    @Inject
    private Cipher cipher;

    @Inject
    private Transfer transfer;
//    @Inject
//    private OSSClient ossClient;

    @Override
    public long store(String sourceName, String destName, InputStream inputStream) {
        destName = DateTimeHelper.appendDateTimeStr(destName, "-" + UUID.randomUUID().toString().substring(0, 9));
        String suffix = getAcceptedSuffix(sourceName);
        long count;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ReadCounterStream readCounterStream = new ReadCounterStream(inputStream)) {
                cipher.encrypt(readCounterStream, outputStream);
                count = readCounterStream.getCount();
            }

            try (ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                transfer.write(destName, encryptedInputStream);
            }
        } catch (TransferException | IOException | EncryptionException e) {
            throw new InternalServerException(FMSErrorCode.UPLOAD_FILE_FAIL, e);
        }

        return repository.storeMetadata(sourceName, destName, "." + suffix, count);
    }

    @Override
    public long storeForCredit(String sourceName, String destName, InputStream inputStream,String userId,String swfName) {
        String suffix = getAcceptedSuffix(sourceName);
        String newName= destName.replaceAll("."+suffix,"");
        StringBuilder sb = new StringBuilder();
        String creditUserName=sb.append("Credit").append("_").append(userId).append("/").toString();
        destName = DateTimeHelper.appendDateTimeStr(newName, "-" + UUID.randomUUID().toString().substring(0, 9));
        String finalName=creditUserName+destName;
        long count;
        try {
            //进行上传接口
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ReadCounterStream readCounterStream = new ReadCounterStream(inputStream)) {
                cipher.encrypt(readCounterStream, outputStream);
                count = readCounterStream.getCount();
            }
            //上传
            try (ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                transfer.writeForCredit(finalName, encryptedInputStream);
            }
        } catch (TransferException | IOException | EncryptionException e) {
            throw new InternalServerException(FMSErrorCode.UPLOAD_FILE_FAIL, e);
        }

        return repository.storeMetadataForCredit(sourceName, finalName, "." + suffix, count,swfName);
    }

    private String getAcceptedSuffix(String name) {
        int pos = name.lastIndexOf(".");
        String suffix = name.substring(pos + 1);
        ensureAcceptedSuffix(suffix);

        return suffix;
    }

    private void ensureAcceptedSuffix(String suffix) {
        if (!ACCEPT_EXTENSIONS.contains(suffix.toLowerCase())) {
            throw new InvalidRequestException(FMSErrorCode.FILE_EXTENSION_NOT_ACCEPT);
        }
    }

    @Override
    public InputStream fetch(String destName) {
        ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream(BUFFER);

        try {
            InputStream encryptedStream = transfer.read(destName);
            cipher.decrypt(encryptedStream, decryptedStream);
        } catch (TransferException | DecryptionException e) {
            throw new InternalServerException(FMSErrorCode.DOWNLOAD_FILE_FAIL, e);
        }

        return new ByteArrayInputStream(decryptedStream.toByteArray());
    }

    @Override
    public InputStream fetchForCredit(String destName) {
        ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream(BUFFER);

        try {
            InputStream encryptedStream = transfer.readForCredit(destName);
            cipher.decrypt(encryptedStream, decryptedStream);
        } catch (TransferException | DecryptionException e) {
            throw new InternalServerException(FMSErrorCode.DOWNLOAD_FILE_FAIL, e);
        }

        return new ByteArrayInputStream(decryptedStream.toByteArray());
    }

    @Override
    public String convertForView(File sourceFile) {
        return ConvertUtil.convert(sourceFile);
    }

    @Override
    public Map doc2swf(String fileString) throws Exception {
        return ConvertUtil.doc2swf(fileString);
    }

    @Override
    public void runOpenOffice() throws Exception {
        ConvertUtil.runOpenOffice();
    }

    @Override
    public String saveUploadFileForView(InputStream inputStreamFile, String destName) {
        return ConvertUtil.saveUploadFileForView(inputStreamFile,destName);
    }

    @Override
    public FileMetadata findMetadataById(long fileId) {
        return repository.findMetadataById(fileId);
    }

    @Override
    public String saveCmsImg(InputStream fileInputStream, String filePath) {
        String dstFilePath = FILE_SERVERS + filePath;
        OutputStream outputStream = null;
        try {
            File file = new File(dstFilePath);
            if(!file.exists()){
                if(!file.getParentFile().exists()){
                    file.getParentFile().mkdirs();
                }
                outputStream = new FileOutputStream(new File(
                        dstFilePath));
                int read = 0;
                byte[] bytes = new byte[1024];

                outputStream = new FileOutputStream(new File(dstFilePath));
                while ((read = fileInputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }else{
                LOGGER.info("the pic already exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                LOGGER.error("close stream defeat");
                e.printStackTrace();
            }
        }
        return  dstFilePath;
    }

    @Override
    public File fetch(List<Long> fileIds, String zipFileName) {
        List<FileMetadata> metadatas = repository.findMetadataByIds(fileIds);
        List<Entry> entries = metadatas.stream().parallel()
                .map(metadata -> {
                    String fileName = metadata.getDestName() + metadata.getSuffix();
                    fileName = fileName.replaceAll(".*/(.*)", "$1");
                    return new Entry(fileName, fetch(metadata.getDestName()));
                }).collect(toList());

        return compressZip(zipFileName, entries);
    }

    @Override
    public File fetchForCredit(List<Long> fileIds, String zipFileName) {
        List<FileMetadata> metadatas = repository.findMetadataByIds(fileIds);
        List<Entry> entries = metadatas.stream().parallel()
                .map(metadata -> {
                    String fileName = metadata.getDestName() + metadata.getSuffix();
                    fileName = fileName.replaceAll(".*/(.*)", "$1");
                    return new Entry(fileName, fetchForCredit(metadata.getDestName()));
                }).collect(toList());
      File newFile=  getFileForView(zipFileName, entries);
        //将新生成的文件名存入数据库
        repository.updateSwfFileNameMetadataById(fileIds.get(0), FilenameUtils.getBaseName(newFile.getAbsolutePath()));
        return newFile;
    }
    private File getFileForView(String zipFileName, List<Entry> entries) {
        String newFilePath = "";
        File newFile=null;
        try {
            byte data[] = new byte[BUFFER];
            for (Entry entry : entries) {
                try (BufferedInputStream origin = new BufferedInputStream(entry.getInputStream())) {
                    LOGGER.info("获取文件存放路径："+FILE_SERVERS);
                    String resfile = "";
                    resfile = System.currentTimeMillis()
                            + zipFileName.substring(
                            zipFileName.lastIndexOf('.'));
                    newFilePath=FILE_SERVERS+"/"+resfile;
                    newFile =new File(newFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(newFilePath)));
                    int itemp = 0;
                    while((itemp = origin.read()) != -1){
                        bos.write(itemp);
                    }
                    System.out.println("文件获取成功"); //console log :文件获取成功
//                    origin.close();
                    bos.close();

                }
            }
        } catch (IOException e) {
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR, e);
        }
        return newFile;
    }

    private File compressZip(String zipFileName, List<Entry> entries) {
       File zipFile = FileBuilder.builder(zipFileName + ".zip").build();
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFFER))) {
            byte data[] = new byte[BUFFER];
            for (Entry entry : entries) {
                try (BufferedInputStream origin = new BufferedInputStream(entry.getInputStream())) {
                    ZipEntry zipEntry = new ZipEntry(entry.getName());
                    out.putNextEntry(zipEntry);
                    int count;
                    while ((count = origin.read(data)) != EOF) {
                        out.write(data, 0, count);
                    }
                }
            }
        } catch (IOException e) {
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR, e);
        }
        return zipFile;
    }

    private static class Entry {
        private InputStream inputStream;
        private String name;

        public Entry(String name, InputStream inputStream) {
            this.inputStream = inputStream;
            this.name = name;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getName() {
            return name;
        }
    }

    private static class ReadCounterStream extends InputStream {
        private InputStream origin;
        private long count;

        ReadCounterStream(InputStream origin) {
            this.origin = origin;
            this.count = 0;
        }

        @Override
        public int read(byte b[], int off, int sz) throws IOException {
            int res = origin.read(b, off, sz);
            if (res != EOF) {
                count += res;
            }

            return res;
        }

        @Override
        public int read() throws IOException {
            int res = origin.read();
            if (res != EOF) {
                this.count += res;
            }

            return res;
        }

        @Override
        public void close() throws IOException {
            if (Objects.nonNull(origin)) {
                origin.close();
            }
        }

        long getCount() {
            return this.count;
        }
    }

    /**
     * ���url����
     *
     * @param key
     * @return
     */
    public String getUrl(String key) {
      // ����URL����ʱ��Ϊ10��  3600l* 1000*24*365*10
      Date expiration = new Date(new Date().getTime() + 3600l * 1000 * 24 * 365 * 10);
      // ����URL
      URL url = client.generatePresignedUrl(BUCKET_NAME, key, expiration);
      if (url != null) {
        return url.toString();
      }
      return null;
    }
  

}
