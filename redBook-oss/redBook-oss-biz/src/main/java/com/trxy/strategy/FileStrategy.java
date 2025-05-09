package com.trxy.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * 策略接口
 * 面向接口编程
 */
public interface FileStrategy {

    /**
     * 文件上传
     *
     * @param file
     * @param bucketName
     * @return
     */
    String uploadFile(MultipartFile file, String bucketName);

}