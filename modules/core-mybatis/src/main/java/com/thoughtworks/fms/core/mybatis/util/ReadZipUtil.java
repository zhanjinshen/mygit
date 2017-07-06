package com.thoughtworks.fms.core.mybatis.util;

/**
 * Created by TanYuan on 2017/7/3.
 */

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.*;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class ReadZipUtil {
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        try {
            readZipFile("F:\\亚飞小贷-李晟-40万-12个月.zip");
         //   getZipFileContent("F:\\亚飞小贷-李晟-40万-12个月.zip","test");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void readZipFile(String file) throws Exception {
        ZipFile zf = new ZipFile(file,"gbk");
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        Enumeration<ZipEntry> entries = zf.getEntries();
        while ((ze = entries.nextElement()) != null) {
            if (ze.isDirectory()) {
                // System.out.print("directory - " + ze.getName() + " : "
                // + ze.getSize() + " bytes");
                // System.out.println();
            } else {
                System.err.println("file - " + ze.getName() + " : "
                        + ze.getSize() + " bytes");
                long size = ze.getSize();
                if (size > 0) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(zf.getInputStream(ze)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                    br.close();
                }
                System.out.println();
            }
        }
        zin.closeEntry();
    }


    /**
     * 读取zip文件中制定文件的内容
     *
     * @param zipFile      目标zip文件对象
     * @param readFileName 目标读取文件名字
     * @return 文件内容
     * @throws ZipException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static String getZipFileContent(String zipFile, String readFileName) throws ZipException, IOException {
        //ZipFile zf = new ZipFile(zipFile);
        StringBuilder content = new StringBuilder();
        ZipFile zip = new ZipFile(zipFile,"gbk");
        Enumeration<ZipEntry> entries = zip.getEntries();
        ZipEntry ze;
        // 枚举zip文件内的文件/
        while (entries.hasMoreElements()) {
            ze = entries.nextElement();
            // 读取目标对象
//            if (ze.getName().equals(readFileName)) {
                Scanner scanner = new Scanner(zip.getInputStream(ze));
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine());
                }
                scanner.close();
//            }
        }
        zip.close();

        return content.toString();
    }

}