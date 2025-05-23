package com.trxy.service.impl;

import com.trxy.config.AliOssProperties;
import com.trxy.response.Response;
import com.trxy.service.FileService;
import com.trxy.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileStrategy fileStrategy;
    @Resource
    private AliOssProperties aliOssProperties;

    @Override
    public String uploadFile(MultipartFile file) {
        // 上传文件到
     return  fileStrategy.uploadFile(file, aliOssProperties.getBucketName());
    }
}