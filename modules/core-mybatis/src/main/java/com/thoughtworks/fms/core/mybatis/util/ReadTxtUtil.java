package com.thoughtworks.fms.core.mybatis.util;

import com.thoughtworks.fms.api.service.ClientService;
import com.thoughtworks.fms.api.service.FileService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TanYuan on 2017/7/10.
 */
public class ReadTxtUtil {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BatchUploadUtil.class);
    private static final String BIGFILE_SERVERS = PropertiesLoader.getProperty("bigfile.servers");
    private static final String FILE_ENCODING =PropertiesLoader.getProperty("file.encoding");
    private static final String FILE_SERVERS = PropertiesLoader.getProperty("file.servers");

    public static void readTxtFile(String fileName, FileService fileService, ClientService clientService) {
        File file = new File(fileName);
        Long startTime = System.currentTimeMillis();
        int i = 0;
        int totalFileNum = 0;
        LOGGER.info("接收到服务器回调的文件路径为" + fileName);
        LOGGER.info("回调处理开始：" + startTime);
        StringBuilder result = new StringBuilder();
        try {
            if (file.isFile() && file.exists()) { //判断文件是否存在
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), FILE_ENCODING);//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt;
                Map<String,String> fileNameMap=new HashMap<>();
                Map<String,String> fileTimeNameMap=new HashMap<>();
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    System.out.println(lineTxt);//逐行读取文件后进行处理
                    String newSwfFileName=System.currentTimeMillis()+"";
                    totalFileNum = i++;
                    LOGGER.info("逐条信息索引为：" + totalFileNum + "逐条读取文件内容，内容为：" + lineTxt);
                    if (lineTxt.indexOf(".swf") < 0) {
                        String sourceFileSwfName=batchUploadFileToOss(fileService, clientService, lineTxt,newSwfFileName);
                        fileNameMap.put(totalFileNum+"",sourceFileSwfName);
                        fileTimeNameMap.put(totalFileNum+"",newSwfFileName);
                        LOGGER.info("源文件："+lineTxt+"对应的新生成的swf文件名字为："+sourceFileSwfName);
                    }else{
                        String swfFileName=fileNameMap.get(totalFileNum-1+"").replaceAll("/","_");
                        LOGGER.info("通过源文件取出的swf文件名为："+swfFileName);
                        LOGGER.info("通过服务器文件读出的swf文件名为："+lineTxt);
                       // String lineTxtSwfFileName=lineTxt.replaceAll(BIGFILE_SERVERS.toString()+"swf/","");
                        if(lineTxt.indexOf(swfFileName)>-1){
                            renameFileAndCallbackCredit(lineTxt,fileTimeNameMap.get(totalFileNum-1+""));
                            LOGGER.info("将文件回调给credit,回调的文件为："+lineTxt);
                        }
                    }
                }
                read.close();
            } else {
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }
//        File zf = new File(fileName);
//        String sourceName= zf.getName();
//        String destName=zf.getName();
//        String source="laiyuan";
//        String url ="111111111";
//        long fileId=1L;
//        InputStream inputStreamForUpload=null;
//        try {
//            inputStreamForUpload=  new FileInputStream(fileName);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        fileId = fileService.storeForCredit(sourceName, destName, inputStreamForUpload, source,url);
//        //credit固定路径
//        String uri = "/creditAttachment/saveCreditAttachmentByFileId";
//        clientService.informCredit(uri, null!=url&&""!=url?Long.valueOf(url):0, sourceName, destName);
        System.out.println("成功回调！");
        Long endTime = System.currentTimeMillis();
        Long consumeTime = endTime - startTime;
        LOGGER.info("回调处理开始时间：" + startTime);
        LOGGER.info("回调处理完成时间：" + endTime);
        LOGGER.info("回调处理完成,总耗时：" + consumeTime);
        LOGGER.info("回调处理完成,处理总文件：" + totalFileNum);
    }

    private static String batchUploadFileToOss(FileService fileService,ClientService clientService,String fileName,String url) throws UnsupportedEncodingException {
        File zf = new File(fileName);
        String sourceName= new String(zf.getName().getBytes("ISO-8859-1"));
        String destName=new String(zf.getPath().getBytes("ISO-8859-1"));
        String source= FilenameUtils.getBaseName(fileName);

//        File f=new File(fileName);
//        String c=f.getParent();
//        File mm=new File(c+File.pathSeparator+System.currentTimeMillis()+".swf");
//        if(f.renameTo(mm))
//        {
//            System.out.println("修改成功!");
//        }
//        else
//        {
//            System.out.println("修改失败");
//        }
//        String url =FilenameUtils.getBaseName(fileName);
        //String url =System.currentTimeMillis()+"";
        long fileId;
        InputStream inputStreamForUpload=null;
        try {
            inputStreamForUpload=  new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        LOGGER.info("将上传到阿里oss服务器的文件进行保存：sourceName-->"+sourceName+"-->destName-->"+destName);
        fileId = fileService.storeForCredit(sourceName, destName, inputStreamForUpload, source,url);
        //credit固定路径
        String uri = "/creditAttachment/saveCreditAttachmentByFileId";

        //在数据库中查询到上传时的来源
        String sourceFileSwfName=fileName.replace(BIGFILE_SERVERS+"/","").replaceAll(sourceName,"")+source+".swf";
        LOGGER.info("--源文件名为："+fileName+"--大文件处理文件路径为："+BIGFILE_SERVERS+"--需要截取的文件名为："+sourceName+"--需要拼接的文件名为："+source);
        LOGGER.info("返回对应的swf文件路径为："+sourceFileSwfName);
        String creditSourceFileName=sourceFileSwfName.substring(0, sourceFileSwfName.indexOf("/"));
        String creditSource= fileService.findBigFileMetadataBySourceName(creditSourceFileName);

        clientService.informCreditBigFile(uri, null!=url&&""!=url?Long.valueOf(url):fileId, sourceName, destName,creditSource);

        return sourceFileSwfName;
    }

    private static String renameFileAndCallbackCredit(String fileName,String newFileName) throws UnsupportedEncodingException {
        try {

            File file = new File(fileName);
            String date= DateTimeHelper.stampToDate(newFileName);
            LOGGER.info("swf源文件名为："+fileName+"对应新生成的swf名字为："+newFileName);
            File swfFile = new File(FILE_SERVERS + "/"+ date+"/" + newFileName + ".swf");
            if (!swfFile.getParentFile().exists()) {
                swfFile.getParentFile().mkdirs();
            }
            FileUtils.copyFile(file,swfFile);
//            if (file.renameTo(swfFile)) {
//                System.out.println("File is moved successful!");
//            } else {
//                System.out.println("File is failed to move!");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //        File zf = new File(fileName);
//        String sourceName= zf.getName();
//        String destName=zf.getPath();
//        String source= FilenameUtils.getBaseName(fileName);
//
//        String url ="";
//        long fileId=1L;
//        InputStream inputStreamForUpload=null;
//        try {
//            inputStreamForUpload=  new FileInputStream(fileName);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        fileId = fileService.storeForCredit(sourceName, destName, inputStreamForUpload, source,url);
//        //credit固定路径
//        String uri = "/creditAttachment/saveCreditAttachmentByFileId";
//        clientService.informCredit(uri, null!=url&&""!=url?Long.valueOf(url):0, sourceName, destName);
//        String sourceFileSwfName=fileName.replace(BIGFILE_SERVERS+"\\","").replaceAll(sourceName,"")+source+".swf";
//        LOGGER.info("返回对应的swf文件路径为："+sourceFileSwfName);
        return "";
    }
}
