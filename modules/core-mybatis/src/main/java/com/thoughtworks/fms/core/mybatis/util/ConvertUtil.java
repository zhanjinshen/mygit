package com.thoughtworks.fms.core.mybatis.util;


import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TanYuan on 2017/4/27.
 */
public class ConvertUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertUtil.class);
    private static final String FILE_SERVERS = PropertiesLoader.getProperty("file.servers");
    private static final String SWFTools_SERVERS= PropertiesLoader.getProperty("swftools.servers");
    private static final String SWFTools_SERVERS_EXECUTE= PropertiesLoader.getProperty("swftools.servers.execute");
    private static final String OPENOFFICE_SERVERS = PropertiesLoader.getProperty("openoffice.servers");
    private static final String START_OPENOFFICE_COMMAND = PropertiesLoader.getProperty("start.OpenOffice.command");
    private static final String OPENOFFICE_SERVERS_IP = PropertiesLoader.getProperty("openoffice.servers.ip");
    private static final String OPENOFFICE_SERVERS_PORT = PropertiesLoader.getProperty("openoffice.servers.port");
    public static String convert(File sourceFile) {
        String url="";
        try {
            String fileName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
           String date= DateTimeHelper.stampToDate(fileName);
            File swfFile = new File(FILE_SERVERS + "/"+ date+"/" + fileName + ".swf");
            LOGGER.info("获取pdf文件路径：" + fileName);
            File targetFile = swfFile;
            ConvertToSwf convertToSwf =new ConvertToSwf(SWFTools_SERVERS,SWFTools_SERVERS_EXECUTE);
            boolean res=convertToSwf.convertFileToSwf(sourceFile.getAbsolutePath(),targetFile.getAbsolutePath());

            if (res){
                url=fileName;
            }
//            /**
//             * SWFTools_HOME在系统中的安装目录
//             * 1：window需要指定到 pdf2swf.exe 文件
//             * 2：linux则xxx/xxx/xxx/pdf2swf即可
//             */
//            String SWFTools_HOME = SWFTools_SERVERS_PDF;
//            LOGGER.info("SWFTools服务路径：" + SWFTools_HOME);
//            String[] cmd = new String[5];
//            cmd[0] = SWFTools_HOME;
//            cmd[1] = "-i";
//            cmd[2] = sourceFile.getAbsolutePath();
//            cmd[3] = "-o";
//            cmd[4] = targetFile.getAbsolutePath();
//            LOGGER.info("*******pdf2swf开始执行文件转换********");
//            Process pro = Runtime.getRuntime().exec(cmd);
//            LOGGER.info("*******pdf2swf文件转换完成********");
//            LOGGER.info("*******pdf2swf文件转换完成，生成的swf文件路径为：" + targetFile.getPath());
////           如果不读取流则targetFile.exists() 文件不存在，但是程序没有问题
////          BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pro.getInputStream()));
////          while (bufferedReader.readLine() != null);
//            pro.waitFor();
//            pro.exitValue();
        } catch (Exception e) {
            System.out.println("pdf转换swf失败");
            return url;
        }
        return url;
    }

    public static Map doc2swf(String fileString) throws Exception {
        Map<String, Object> fileMap = new HashMap<>();
        LOGGER.info("转换开始，转换文件在文件服务器路径：" + fileString);
        String fileName = fileString.substring(0, fileString.lastIndexOf("."));
        File docFile = new File(fileString);
        File pdfFile = new File(fileName + ".pdf");
        if (docFile.exists()) {
            if (!pdfFile.exists()) {
                try {
                    //doc2pdf
                    //run openoffice
                    boolean isUse=NetUtil.isPortUsing(OPENOFFICE_SERVERS_IP,Integer.valueOf(OPENOFFICE_SERVERS_PORT));
                    if(!isUse){
                    runOpenOffice();
                    }
//                    OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
//                    connection.connect();
//
//                    DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
//                    // 2:获取Format
//                    DocumentFormatRegistry factory = new ConvertToSwf();
//                    DocumentFormat inputDocumentFormat = factory
//                            .getFormatByFileExtension(FilenameUtils.getExtension(docFile.getAbsolutePath()));
//                    DocumentFormat outputDocumentFormat = factory
//                            .getFormatByFileExtension(FilenameUtils.getExtension(pdfFile.getAbsolutePath()));
//                    // 3:执行转换
//                    converter.convert(docFile, inputDocumentFormat, pdfFile, outputDocumentFormat);
                    LOGGER.info("OpenOffice启动成功");
                    OpenOfficeConnection connection = new SocketOpenOfficeConnection(OPENOFFICE_SERVERS_IP,Integer.valueOf(OPENOFFICE_SERVERS_PORT));
                    LOGGER.info("开始监听"+OPENOFFICE_SERVERS_PORT+"端口");
                    LOGGER.info("connection开始");
                    connection.connect();
                    LOGGER.info("connection通道打开");
                    DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
                    LOGGER.info("*******doc2pdf开始执行文件转换********");
                    converter.convert(docFile, pdfFile);
                    LOGGER.info("*******doc2pdf文件转换完成********");
                    // close the connection
                    LOGGER.info("connection通道关闭");
                    connection.disconnect();
                    LOGGER.info("使用openOffice将文件转成pdf成功" + pdfFile.getPath() + "****");
                    //pdf2swf
//                    if (!swfFile.exists()) {
//                        String pdf2swfCommand =  "E:\\SWFTools\\pdf2swf.exe  -i " + pdfFile + " -o "  + swfFile;
//                        Runtime.getRuntime().exec(pdf2swfCommand);
//                        System.out.println("将pdf转成swf成功,文件路径为"+ swfFile.getPath() + "****");
//                    }
                    //delete pdf file
//                    pdfFile.delete();
                    fileMap.put("pdfFile", pdfFile);
                    fileMap.put("docFile", docFile);
                } catch (java.net.ConnectException e) {
                    e.printStackTrace();
                    LOGGER.error("使用openOffice将文件转成pdf失败java.net.ConnectException",e);
                    throw e;
                } catch (com.artofsolving.jodconverter.openoffice.connection.OpenOfficeException e) {
                    e.printStackTrace();
                    LOGGER.error("使用openOffice将文件转成pdf失败com.artofsolving.jodconverter.openoffice.connection.OpenOfficeException",e);
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                LOGGER.info("pdf文件已存在无需转换！");
            }
        } else {
            LOGGER.info("doc文件不存在！");
        }
        return fileMap;
    }

    public static void runOpenOffice() throws Exception {
        try {
            //�ж�soffice.exe�Ƿ�����
//            boolean isSofficeRun = false;
//            String testProcessRunCommand =OPENOFFICE_RUN_COMMAND;
//            InputStream is = null;
//            InputStreamReader ir = null;
//            BufferedReader br = null;
//            String line = null;
//            Process pro = Runtime.getRuntime().exec(testProcessRunCommand);
//            is = pro.getInputStream();
//            ir = new InputStreamReader(is);
//            br = new BufferedReader(ir);
//            while ((line = br.readLine()) != null) {
//                if (line.indexOf("soffice.exe") != -1) {
//                    isSofficeRun = true;
//                    break;
//                }
//            }
//            if(!isSofficeRun){
            // ����OpenOffice�ķ���
//                String OpenOffice_HOME = "D:\\Program Files\\OpenOffice 4.1.3\\program\\";
//                String OpenOffice_HOME = "C:\\Program Files (x86)\\OpenOffice 4";// 这里是OpenOffice的安装目录,
            String OpenOffice_HOME = OPENOFFICE_SERVERS;// 这里是OpenOffice的安装目录,
            LOGGER.info("获取配置的OpenOffice安装路径:" + OPENOFFICE_SERVERS);
            // 在我的项目中,为了便于拓展接口,没有直接写成这个样子,但是这样是尽对没题目的
            // 假如从文件中读取的URL地址最后一个字符不是 '\'，则添加'\'
            if (OpenOffice_HOME.charAt(OpenOffice_HOME.length() - 1) != '/') {
                OpenOffice_HOME += "/";
            }
            String startOpenOfficecommand = OpenOffice_HOME + START_OPENOFFICE_COMMAND;
            LOGGER.info("获取OpenOffice启动命令并且启动:" + startOpenOfficecommand);
            Process pro = Runtime.getRuntime().exec(startOpenOfficecommand);
//            }
 //               pro.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static String saveUploadFileForView(InputStream inputStreamFile, String destName) {
        String newFileName = "";
        String newPathname = "";
        String fileAddre = "/numUp";
        try {
//            InputStream stream = multiPart.getField("file").getValueAs(InputStream.class);// 把文件读入
            InputStream stream = inputStreamFile;// 把文件读入
            String filePath = FILE_SERVERS;//取系统当前路径
            File file1 = new File(filePath);//添加了自动创建目录的功能
            ((File) file1).mkdir();
            newFileName = System.currentTimeMillis()
                    + destName.substring(
                    destName.lastIndexOf('.'));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream bos = new FileOutputStream(filePath + "/"
                    + newFileName);
            newPathname = filePath + "/" + newFileName;
            //新生成的文件路径
            System.out.println(newPathname);
            // 建立一个上传文件的输出流
            System.out.println(filePath + "/" + destName);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, bytesRead);// 将文件写入服务器
            }
            bos.close();
           stream.close();
            return newPathname;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return newPathname;
        }

    }
}
