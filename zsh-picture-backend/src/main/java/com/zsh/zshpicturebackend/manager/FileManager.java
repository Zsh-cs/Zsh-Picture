package com.zsh.zshpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zsh.zshpicturebackend.config.CosClientConfig;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.dto.file.PictureUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {

    @Autowired
    private CosClientConfig cosClientConfig;
    @Autowired
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public PictureUploadResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        verifyPicture(multipartFile);
        // 生成图片上传地址
        String uuid = RandomUtil.randomString(16);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(multipartFile.getOriginalFilename()));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 创建本地临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cos_upload_", null);
            // 把前端传过来的文件内容写入这个临时文件
            multipartFile.transferTo(tempFile);
            // 调用cosManager的上传并解析图片方法
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double scale= NumberUtil.round((double) width/height,2).doubleValue();
            // 封装到返回结果中
            PictureUploadResult pictureUploadResult = new PictureUploadResult();
            pictureUploadResult.setUrl(cosClientConfig.getHost() + uploadPath);
            pictureUploadResult.setName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            pictureUploadResult.setPicSize(multipartFile.getSize());
            pictureUploadResult.setPicWidth(width);
            pictureUploadResult.setPicHeight(height);
            pictureUploadResult.setPicScale(scale);
            pictureUploadResult.setPicFormat(imageInfo.getFormat());
            return pictureUploadResult;
        } catch (Exception e) {
            log.error("图片上传到COS失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传到COS失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    // 校验图片
    private void verifyPicture(MultipartFile multipartFile) {
        // 图片不能为空
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        // 图片体积不能超过2MB
        if (multipartFile.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片体积超过2MB");
        }
        // 图片后缀必须在允许的列表内
        final List<String> ALLOW_SUFFIX = Arrays.asList("jpg", "jpeg", "png", "webp");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (!ALLOW_SUFFIX.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不在允许的列表内");
        }
    }

    // 清理临时文件
    public static void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean delete = tempFile.delete();
            if (!delete) {
                log.error("tempFile delete error, path={}", tempFile.getAbsolutePath());
            }
        }
    }
}
