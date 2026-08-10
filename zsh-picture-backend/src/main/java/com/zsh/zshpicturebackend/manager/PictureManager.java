package com.zsh.zshpicturebackend.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.io.unit.DataUnit;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zsh.zshpicturebackend.config.CosClientConfig;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.dto.picture.PictureUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 模板方法设计模式
 */
@Slf4j
public abstract class PictureManager {

    @Autowired
    private CosManager cosManager;
    @Autowired
    private CosClientConfig cosClientConfig;

    public final PictureUploadResult uploadPicture(Object inputSource, String uploadPathPrefix){
        // 1.校验输入源（本地图片或url图片），返回图片后缀
        String suffix = verify(inputSource);

        // 2.生成图片上传到COS后在桶中的路径
        String uuid= RandomUtil.randomString(16);
        String originalPictureName=getOriginalPictureName(inputSource);
        if(suffix.isEmpty()){
            suffix=FileUtil.getSuffix(originalPictureName);
        }
        String uploadPictureName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid, suffix);
        String uploadPath=String.format("%s/%s",uploadPathPrefix,uploadPictureName);
        // 原图url
        String originalUrl=cosClientConfig.getHost()+"/"+uploadPath;

        File tempFile=null;
        try{
            // 3.创建临时文件
            tempFile=File.createTempFile("cos_upload_",null);
            // 4.将要上传的图片转移到临时文件中
            transferToTempFile(inputSource,tempFile);
            // 5.将临时文件上传到COS
            PutObjectResult res = cosManager.putPictureObject(uploadPath, tempFile);

            // 6.封装返回结果
            PictureUploadResult pictureUploadResult = new PictureUploadResult();
            // 先统一设置好原图url
            pictureUploadResult.setOriginalUrl(originalUrl);
            // 获取数据万象CI处理图片后的结果
            CIUploadResult ciUploadResult = res.getCiUploadResult();
            List<CIObject> objectList = ciUploadResult.getProcessResults().getObjectList();
            // 如果对象列表不为空，说明图片压缩成功，将压缩后的图片封装成返回结果
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedPicture = objectList.get(0);
                int width = compressedPicture.getWidth();
                int height = compressedPicture.getHeight();
                double scale = NumberUtil.round((double) width / height, 2).doubleValue();
                // getKey获取的是这张压缩图片在桶中的路径：public/用户id/xxx.webp
                pictureUploadResult.setUrl(cosClientConfig.getHost()+"/"+compressedPicture.getKey());
                pictureUploadResult.setName(FileUtil.mainName(originalPictureName));
                pictureUploadResult.setPicSize(compressedPicture.getSize().longValue());
                pictureUploadResult.setPicWidth(width);
                pictureUploadResult.setPicHeight(height);
                pictureUploadResult.setPicScale(scale);
                pictureUploadResult.setPicFormat(compressedPicture.getFormat());
            } else {
                // 否则说明图片压缩失败，将原图封装成返回结果
                ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
                int width = imageInfo.getWidth();
                int height = imageInfo.getHeight();
                double scale = NumberUtil.round((double) width / height, 2).doubleValue();
                pictureUploadResult.setUrl(originalUrl);
                pictureUploadResult.setName(FileUtil.mainName(originalPictureName));
                pictureUploadResult.setPicSize(FileUtil.size(tempFile));
                pictureUploadResult.setPicWidth(width);
                pictureUploadResult.setPicHeight(height);
                pictureUploadResult.setPicScale(scale);
                pictureUploadResult.setPicFormat(imageInfo.getFormat());
            }
            return pictureUploadResult;
        } catch (Exception e) {
            log.error("图片上传到COS失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图片上传到COS失败");
        } finally {
            // 7.删除临时文件
            deleteTempFile(tempFile);
        }
    }

    // 校验输入源（本地图片或url图片），返回图片后缀
    protected abstract String verify(Object inputSource);

    // 获取原始图片名
    protected abstract String getOriginalPictureName(Object inputSource);

    // 将要上传的图片转移到临时文件中
    protected abstract void transferToTempFile(Object inputSource, File tempFile) throws Exception;

    // 删除临时文件
    public void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean res = tempFile.delete();
            if (!res) {
                log.error("tempFile delete error, path={}", tempFile.getAbsolutePath());
            }
        }
    }
}
