package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author mqx
 * @date 2020/4/18 15:28
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {

    // 文件上传完成之后返回文件地址：
    // http://img12.360buyimg.com/n7/jfs/t1/97129/16/18957/134723/5e9977a9Ea874f1c3/d8df81e4e62d8de3.jpg
    // 配置文件服务的Ip地址,放在配置文件中，实现了软编码。
    @Value("${fileServer.url}")
    private String fileUrl; // fileUrl=http://192.168.200.128:8080/

    // springMVC 自动封装了一个文件上传的类。 file 是前端页面指定好的。

    /**
     *
     * @param file 用户点击的图片文件
     * @return
     */
    @RequestMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws IOException, MyException {
        // 获取resource 目录下的tracker.conf 注意：项目目录中千万不能有中文！
        String configFile = this.getClass().getResource("/tracker.conf").getFile();

        // 什么图片返回的路径
        String path = null;
        if (configFile!=null){
            // 初始化文件
            ClientGlobal.init(configFile);
            // 文件上传 需要tracker,storage
            TrackerClient trackerClient = new TrackerClient();
            // 获取trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();

            // 获取storageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 上传文件
            // 第一个参数表示要上传文件的字节数组
            // 第二个参数：文件的后缀名
            // 第三个参数： 数组，null
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);

            // 上传完成之后，需要获取到文件的上传路径
            System.out.println("图片路径："+fileUrl +path);
        }

        return Result.ok(fileUrl+path);
    }
}
