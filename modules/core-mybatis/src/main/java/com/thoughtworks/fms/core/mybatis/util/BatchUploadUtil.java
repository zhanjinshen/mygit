package com.thoughtworks.fms.core.mybatis.util;

/**
 * Created by TanYuan on 2017/6/30.
 */

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * Servlet implementation class PluploadServlet
 */
//    @WebServlet("/uploadFileForCreditForBatchUpload")
public class BatchUploadUtil extends HttpServlet {
    private static final String BIGFILE_SERVERS = PropertiesLoader.getProperty("bigfile.servers");

    private static final String BIGFILE_HANDLE_SCRIPT = PropertiesLoader.getProperty("bigfile.handle.script");

    private static final long serialVersionUID = 1L;

    private static final int BUFFER_SIZE = 100 * 1024;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BatchUploadUtil.class);


//        public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//            doPost(request, response);
//        }

    public static int getBatchUpload(FormDataMultiPart multiPart, InputStream fileInputStream, HttpServletRequest request) {
        try {
//                String name = request.getParameter("name");
            int res;
            String name = multiPart.getField("name").getValueAs(String.class);
            Integer chunk = 0, chunks = 0;
            chunk = multiPart.getField("chunk").getValueAs(Integer.class);
            chunks = multiPart.getField("chunks").getValueAs(Integer.class);
            if (null != request.getParameter("chunk") && !request.getParameter("chunk").equals("")) {
                chunk = Integer.valueOf(request.getParameter("chunk"));
            }
            if (null != request.getParameter("chunks") && !request.getParameter("chunks").equals("")) {
                chunks = Integer.valueOf(request.getParameter("chunks"));
            }
            LOGGER.info("chunk:[" + chunk + "] chunks:[" + chunks + "]");
            //检查文件目录，不存在则创建
            //String relativePath = "/plupload/files/";
            //String realPath = request.getServletContext().getRealPath("");
//                File folder = new File(realPath + relativePath);
//                if (!folder.exists()) {
//                    folder.mkdirs();
//                }

            //目标文件
            File destFile = new File(BIGFILE_SERVERS, name);
            //文件已存在删除旧文件（上传了同名的文件）
            if (chunk == 0 && destFile.exists()) {
                destFile.delete();
                destFile = new File(BIGFILE_SERVERS, name);
            }
            //合成文件
            appendFile(fileInputStream, destFile);
            if (chunk == chunks - 1) {
                LOGGER.info("上传完成");

                return 1;

            } else {
                LOGGER.info("还剩[" + (chunks - 1 - chunk) + "]个块文件");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return 0;

    }
//        public static void getBatchUpload(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
//            try {
//                boolean isMultipart = ServletFileUpload.isMultipartContent(request);
//                if(isMultipart){
//                    String fileName = "";
//                    Integer chunk = 0, chunks = 0;
//
//                    //检查文件目录，不存在则创建
//                    String relativePath = "/plupload/files/";
//                    String realPath = request.getServletContext().getRealPath("");
//                    File folder = new File(realPath + relativePath);
//                    if (!folder.exists()) {
//                        folder.mkdirs();
//                    }
//
//                    DiskFileItemFactory diskFactory = new DiskFileItemFactory();
//                    // threshold 极限、临界值，即硬盘缓存 1M
//                    diskFactory.setSizeThreshold(4 * 1024);
//
//                    ServletFileUpload upload = new ServletFileUpload(diskFactory);
//                    // 设置允许上传的最大文件大小（单位MB）
//                    upload.setSizeMax(1024 * 1048576);
//                    upload.setHeaderEncoding("UTF-8");
//
//                    try {
////                        StandardMultipartHttpServletRequest req = (StandardMultipartHttpServletRequest) request;
////                        MultipartFormDataRequest mrequest = new MultipartFormDataRequest(request);
//
//                        FileItemIterator it = upload.getItemIterator(request);
//
////                        List<FileItem> fileList = upload.parseRequest(request);
////                        Iterator<FileItem> it = fileList.iterator();
//                        while (it.hasNext()) {
//                            FileItem item = (FileItem)it.next();
//                            String name = item.getFieldName();
//                            InputStream input = item.getInputStream();
//                            if("name".equals(name)){
//                                fileName = Streams.asString(input);
//                                continue;
//                            }
//                            if("chunk".equals(name)){
//                                chunk = Integer.valueOf(Streams.asString(input));
//                                continue;
//                            }
//                            if("chunks".equals(name)){
//                                chunks = Integer.valueOf(Streams.asString(input));
//                                continue;
//                            }
//                            // 处理上传文件内容
//                            if (!item.isFormField()) {
//                                //目标文件
//                                File destFile = new File(folder, fileName);
//                                //文件已存在删除旧文件（上传了同名的文件）
//                                if (chunk == 0 && destFile.exists()) {
//                                    destFile.delete();
//                                    destFile = new File(folder, fileName);
//                                }
//                                //合成文件
//                                appendFile(input, destFile);
//                                if (chunk == chunks - 1) {
//                                    LOGGER.info("上传完成");
//                                }else {
//                                    LOGGER.info("还剩["+(chunks-1-chunk)+"]个块文件");
//                                }
//                            }
//                        }
//                    } catch (FileUploadException ex) {
//                        LOGGER.warn("上传文件失败：" + ex.getMessage());
//                        return;
//                    }
//                }
//            } catch (IOException e) {
//                LOGGER.error(e.getMessage());
//            }
//        }

    private static void appendFile(InputStream in, File destFile) {
        OutputStream out = null;
        try {
            // plupload 配置了chunk的时候新上传的文件append到文件末尾
            if (destFile.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(destFile, true), BUFFER_SIZE);
            } else {
                out = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
            }
            in = new BufferedInputStream(in, BUFFER_SIZE);

            int len = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

}
