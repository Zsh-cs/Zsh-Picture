package com.zsh.zshpicturebackend.service;

import com.zsh.zshpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author asus
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2026-07-26 17:36:13
 */
public interface PictureService extends IService<Picture> {

    // 上传图片
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

}
