package com.zsh.zshpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class LocalPictureManager extends PictureManager{

    @Override
    protected void verify(Object inputSource) {
        MultipartFile localPicture=(MultipartFile) inputSource;
        ThrowUtils.throwIf(localPicture==null, ErrorCode.PARAMS_ERROR,"图片为空");
        // 图片大小不能超过2MB
        if (localPicture.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小超过2MB");
        }
        // 图片后缀必须在允许的列表内
        String suffix = FileUtil.getSuffix(localPicture.getOriginalFilename());
        if (!ALLOW_SUFFIX.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不在允许的列表内");
        }
    }

    @Override
    protected String getOriginalPictureName(Object inputSource) {
        MultipartFile localPicture=(MultipartFile) inputSource;
        return localPicture.getOriginalFilename();
    }

    @Override
    protected void transferToTempFile(Object inputSource, File tempFile) throws Exception {
        MultipartFile localPicture=(MultipartFile) inputSource;
        localPicture.transferTo(tempFile);
    }
}
