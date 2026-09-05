package com.zsh.zshpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceRankAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceUserAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceAnalyzeRequest;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.space.analysis.*;

import java.util.List;

public interface SpaceAnalysisService {

    // 校验空间分析权限
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    // 根据空间分析请求填充queryWrapper
    void fillQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper);

    // 分析空间使用情况
    SpaceUsageVO analyzeSpaceUsage(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    // 空间图片分类分析
    List<SpaceCategoryAnalyzeVO> analyzeSpaceByPictureCategory(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    // 空间图片标签分析
    List<SpaceTagAnalyzeVO> analyzeSpaceByPictureTag(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    // 空间图片大小分析
    List<SpaceSizeAnalyzeVO> analyzeSpaceByPictureSize(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    // 空间用户上传行为分析
    List<SpaceUserAnalyzeVO> analyzeSpaceUser(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    // 空间使用排行分析
    List<Space> analyzeSpaceRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest);
}
