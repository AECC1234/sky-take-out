package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.MinioOSSOperator;
import com.sky.utils.MinioUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private MinioUtils minioUtils;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("文件上传: {}", file.getOriginalFilename());
        String objectName = minioUtils.uploadAvatar(file, minioUtils.getBucketName());
        return Result.success(minioUtils.getAvatarUrl(objectName, minioUtils.getBucketName()));
    }
}
