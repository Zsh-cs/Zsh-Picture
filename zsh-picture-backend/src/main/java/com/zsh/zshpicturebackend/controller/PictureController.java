package com.zsh.zshpicturebackend.controller;

import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.LoginUserVO;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Autowired
    private PictureService pictureService;
    @Autowired
    private UserService userService;

    // 上传图片（可以重新上传：基础信息不变，只改变图片文件）
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file")MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
}
