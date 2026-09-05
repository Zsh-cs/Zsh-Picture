package com.zsh.zshpicturebackend.controller;

import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceRankAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceUserAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceAnalyzeRequest;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.space.analysis.*;
import com.zsh.zshpicturebackend.service.SpaceAnalysisService;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalysisController {

    @Autowired
    private SpaceAnalysisService spaceAnalysisService;
    @Autowired
    private UserService userService;

    // 分析空间使用情况
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageVO> analyzeSpaceUsage(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageVO spaceUsageVO = spaceAnalysisService.analyzeSpaceUsage(spaceAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageVO);
    }

    // 空间图片分类分析
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVO>> analyzeSpaceByPictureCategory(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeVO> spaceCategoryAnalyzeVOList = spaceAnalysisService.analyzeSpaceByPictureCategory(spaceAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeVOList);
    }

    // 空间图片标签分析
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeVO>> analyzeSpaceByPictureTag(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeVO> spaceTagAnalyzeVOList = spaceAnalysisService.analyzeSpaceByPictureTag(spaceAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyzeVOList);
    }

    // 空间图片大小分析
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeVO>> analyzeSpaceByPictureSize(@RequestBody SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeVO> spaceSizeAnalyzeVOList = spaceAnalysisService.analyzeSpaceByPictureSize(spaceAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeVOList);
    }

    // 空间用户上传行为分析
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeVO>> analyzeSpaceUser(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                   HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserAnalyzeRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeVO> spaceUserAnalyzeVOList = spaceAnalysisService.analyzeSpaceUser(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyzeVOList);
    }

    // 空间使用排行分析：仅管理员
    @PostMapping("/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> analyzeSpaceRank(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest){
        ThrowUtils.throwIf(spaceRankAnalyzeRequest==null,ErrorCode.PARAMS_ERROR);
        List<Space> spaces = spaceAnalysisService.analyzeSpaceRank(spaceRankAnalyzeRequest);
        return ResultUtils.success(spaces);
    }

}
