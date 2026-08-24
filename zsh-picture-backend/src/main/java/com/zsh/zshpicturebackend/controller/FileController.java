package com.zsh.zshpicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Autowired
    private CosManager cosManager;

    // 测试文件上传
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 生成安全的COS存储路径
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (suffix.isEmpty()) {
            suffix = ".tmp";
        }
        String newFilename = UUID.randomUUID() + suffix;
        String filepath = "/test/" + newFilename;

        // 创建本地临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cos_upload_", null);
            // 把前端传过来的文件内容写入这个临时文件
            multipartFile.transferTo(tempFile);
            // 调用cosManager的上传文件方法
            cosManager.putObject(filepath, tempFile);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath={}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败",e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("tempFile delete error, path={}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    // 测试文件下载
    @GetMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInputStream = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);
            // 设置响应头，让浏览器知道你是要下载文件
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            ServletOutputStream responseOutputStream = response.getOutputStream();
            responseOutputStream.write(bytes);
            responseOutputStream.flush();
        } catch (Exception e) {
            log.error("file download error, filepath={}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败",e);
        } finally {
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }
        }
    }
}
