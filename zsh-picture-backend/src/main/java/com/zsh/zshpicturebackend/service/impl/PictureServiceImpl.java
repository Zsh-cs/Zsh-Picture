package com.zsh.zshpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.manager.FileManager;
import com.zsh.zshpicturebackend.model.dto.file.PictureUploadResult;
import com.zsh.zshpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.mapper.PictureMapper;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * @author asus
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-07-26 17:36:13
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Autowired
    private FileManager fileManager;
    @Autowired
    private UserService userService;

    // 上传图片
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"未登录用户不可以上传图片");
        }

        // 默认是新增图片，所以pictureId为空
        Long pictureId=null;
        if(pictureUploadRequest!=null){
            pictureId=pictureUploadRequest.getId();
        }
        // 如果是更新图片，要去数据库查询pictureId对应的图片是否存在
        if(pictureId!=null && !this.lambdaQuery().eq(Picture::getId,pictureId).exists()){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"对应的图片不存在");
        }
        // 上传图片，先上传到公共空间，路径前缀为public/用户id，这样可以区分不同用户上传的图片
        PictureUploadResult pictureUploadResult = fileManager.uploadPicture(multipartFile, String.format("public/%s", loginUser.getId()));

        // 操作数据库
        Picture picture=new Picture();
        BeanUtils.copyProperties(pictureUploadResult,picture);
        picture.setUserId(loginUser.getId());
        // 如果是更新图片，需要补充id和编辑时间
        if(pictureId!=null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 根据图片id进行判断，存在则更新图片，否则新增图片
        boolean res = this.saveOrUpdate(picture);
        if(!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 获取新增或更新后数据库中的图片对象，因为它包含了createTime、editTime、updateTime
        Picture pictureInDB = this.getById(picture.getId());// 主键回填
        return PictureVO.objToVO(pictureInDB);
    }
}




