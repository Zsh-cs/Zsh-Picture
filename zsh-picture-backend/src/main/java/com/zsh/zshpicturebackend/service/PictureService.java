package com.zsh.zshpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsh.zshpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zsh.zshpicturebackend.model.dto.picture.PictureReviewRequest;
import com.zsh.zshpicturebackend.model.dto.picture.PictureReuploadRequest;
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
    PictureVO uploadPicture(MultipartFile multipartFile, PictureReuploadRequest pictureReuploadRequest, User loginUser);

    // Picture转PictureVO
    PictureVO obj2vo(Picture picture);

    // Picture转暂不包含UserVO的PictureVO
    PictureVO obj2incompleteVO(Picture picture);

    // PictureVO转Picture
    Picture vo2obj(PictureVO pictureVO);

    // 将查询请求转化为QueryMapper对象
    QueryWrapper<Picture> getQueryMapper(PictureQueryRequest request);

    // 分页获取PictureVO对象
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    // 校验图片
    void verifyPicture(Picture picture);

    // 审核图片
    boolean reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    // 填充审核参数
    void fillReviewParams(Picture picture, User loginUser);
}
