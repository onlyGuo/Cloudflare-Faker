package com.guoshengkai.cloudflarefaker.utils;

import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    public static String readFile(File str) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(str)){
            for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
                out.write(buffer, 0, i);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        try {
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void readFile(File str, FileReaderHandler handler) {
        byte[] buffer = new byte[4096];
        try (FileInputStream in = new FileInputStream(str)){
            for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
                handler.read(Arrays.copyOf(buffer, i));
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void readStream(InputStream in, FileReaderHandler handler) throws IOException{
        byte[] buffer = new byte[4096];
        for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
            handler.read(Arrays.copyOf(buffer, i));
        }
    }

    public static byte[] readFile2Byte(File file){
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(file)){
            for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
                out.write(buffer, 0, i);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    public static void writeFile(byte[] bytes, File file) {
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copy(File source, File target) {
        if (!target.getParentFile().exists()){
            target.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(target)){
            readFile(source, out::write);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static void writeFolder(String path, File dest) {
        // 先获得资源文件
        List<String> projectResources = getProjectResources(path);
        for (String p: projectResources){
            writeFile(readResource(p), new File(dest, p.split(path)[1]));
        }
    }

    /**
     * 获得自身项目指定路径下的所有资源文件
     * @param path
     *      项目资源路径
     * @return
     */
    @SneakyThrows
    public static List<String> getProjectResources(String path){
        List<String> result = new LinkedList<>();
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
        while (resources.hasMoreElements()){
            URL url = resources.nextElement();
            String protocol = url.getProtocol();//大概是jar
            if ("jar".equalsIgnoreCase(protocol)) {
                //转换为JarURLConnection
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                if (connection != null) {
                    JarFile jarFile = connection.getJarFile();
                    if (jarFile != null) {
                        //得到该jar文件下面的类实体
                        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                        while (jarEntryEnumeration.hasMoreElements()) {
                            JarEntry entry = jarEntryEnumeration.nextElement();
                            String jarEntryName = entry.getName();
                            //这里我们需要过滤不是class文件和不在basePack包名下的类
                            if (jarEntryName.startsWith(path) &&
                                    !jarEntryName.startsWith(path + "/.idea") &&
                                    !jarEntryName.endsWith("/")){
                                result.add(jarEntryName);
                            }
                        }
                    }
                }
            }else if("file".equalsIgnoreCase(protocol)){
                //从maven子项目中扫描
                File file = new File(url.getFile());
                result = getFolderResources(file, path);
            }
        }
        return result;
    }

    private static List<String> getFolderResources(File file, String split){
        LinkedList<String> result = new LinkedList<>();
        if (file.isDirectory()){
            File[] files = file.listFiles();
            for (File sf: files){
                result.addAll(getFolderResources(sf, split));
            }
        }else{
            String fileName = file.getAbsolutePath();
            if (fileName.contains(split)){
                fileName = split + fileName.split(split)[1];
            }
            result.add(fileName);
        }
        return result;
    }

    public static void writeStream(InputStream inputStream, File source) {
        if (!source.getParentFile().exists()){
            source.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(source)){
            readStream(inputStream, out::write);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage mergeImage(List<BufferedImage> bufferedImages) {
        int height = 0;
        int width = 0;
        for (BufferedImage bufferedImage: bufferedImages){
            height += bufferedImage.getHeight();
            width = Math.max(width, bufferedImage.getWidth());
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int y = 0;
        for (BufferedImage bufferedImage: bufferedImages){
            image.createGraphics().drawImage(bufferedImage, 0, y, null);
            y += bufferedImage.getHeight();
        }
        return image;
    }


    /**
     * 检查字符串是否为数字
     * @param str 待检查的字符串
     * @return 是否为数字
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static void writeImage(File file, BufferedImage screenshot) {
        if (!file.getAbsoluteFile().getParentFile().exists()){
            file.getAbsoluteFile().getParentFile().mkdirs();
        }
        try {
            javax.imageio.ImageIO.write(screenshot, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public interface FileReaderHandler{
        void read(byte[] bytes) throws IOException;
    }

    public static String getFristXmlNodeContent(String xmlContent, String nodeName){
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        NodeList nodeList = document.getElementsByTagName(nodeName);
        if (nodeList.getLength() == 0){
            return null;
        }
        return nodeList.item(0).getTextContent();
    }

    /**
     * 复制一个目录及其子目录、文件到另外一个目录
     * @param src
     *      源目录
     * @param dest
     *      目标目录
     * @throws IOException
     */
    public static void copyFolder(File src, File dest){
        if (src.isDirectory()) {
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            if (!dest.exists()) {
                System.out.println("mkdir: " + dest.getAbsolutePath());
                dest.mkdirs();
            }
            String[] files = src.list();
            if (null == files){
                return;
            }
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // 递归复制
                copyFolder(srcFile, destFile);
            }
        } else {
            System.out.println("write: " + dest.getAbsolutePath());
            try (InputStream in = new FileInputStream(src);OutputStream out = new FileOutputStream(dest)){
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }
    }

    public static void zip(File source, File target){
        if (!target.getParentFile().exists()){
            if (!target.getParentFile().mkdirs()){
                throw new RuntimeException("文件[" + source.getParentFile() + "]创建失败");
            }
        }
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))){
            if (source.isDirectory()){
                for (File file: Objects.requireNonNull(source.listFiles())){
                    zip(file, out, file.getName());
                }
            }else{
                zip(source, out, null);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static void unzip(InputStream stream, File target){
        if (!target.exists()){
            if (!target.mkdirs()){
                throw new RuntimeException("文件[" + target + "]创建失败");
            }
        }
        try (ZipInputStream in = new ZipInputStream(stream)){
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()){
                if (entry.isDirectory()){
                    File file = new File(target, entry.getName());
                    if (!file.exists()){
                        file.mkdirs();
                    }
                }else{
                    try(FileOutputStream out = new FileOutputStream(new File(target, entry.getName()))) {
                        readStream(in, out::write);
                    }
                }
                in.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public static void unzip(File source, File target){
        unzip(new FileInputStream(source), target);
    }

    private static void zip(File source, ZipOutputStream out, String name) throws IOException{
        if (null == name){
            name = source.getName();
        }
        if (source.isDirectory()){
            out.putNextEntry(new ZipEntry(name + "/"));
            for (File file: Objects.requireNonNull(source.listFiles())){
                zip(file, out, name + "/" + file.getName());
            }
        }else{
            out.putNextEntry(new ZipEntry(name));
            readFile(source, out::write);
        }
    }

    public static byte[] readResource(String path){
        ClassPathResource classPathResource = new ClassPathResource(path);
        byte[] bs = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(InputStream in = classPathResource.getInputStream()){
            for (int i = in.read(bs); i > 0; i = in.read(bs)){
                out.write(bs, 0, i);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    public static void deleteAll(File parentFile) {
        File[] files = parentFile.listFiles();
        if (null != files){
            for (File file: files){
                if (file.isDirectory()) {
                    deleteAll(file);
                }
                if (!file.delete()){
                    throw new RuntimeException("Clean File Error: " + file.getAbsolutePath());
                }
            }
        }
    }
    public static void rm(File file) {
        deleteAll(file);
        file.delete();
    }

    public static BufferedImage readImage(File file) {
        try {
            return javax.imageio.ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage readImageFromResource(String path) {
        try {
            return javax.imageio.ImageIO.read(new ClassPathResource(path).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
    }
}
