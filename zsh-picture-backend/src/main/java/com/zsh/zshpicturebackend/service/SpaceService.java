package com.zsh.zshpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsh.zshpicturebackend.model.dto.space.SpaceAddRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.space.SpaceVO;

/**
 * @author asus
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-08-13 00:22:30
 */
public interface SpaceService extends IService<Space> {

    // SpaceVO转Space
    Space vo2obj(SpaceVO spaceVO);

    // Space转暂不包含UserVO的SpaceVO
    SpaceVO obj2incompleteVO(Space space);

    // Space转SpaceVO
    SpaceVO obj2vo(Space space);

    // 校验空间
    void verifySpace(Space space);

    // 根据空间级别自动填充限额数据
    void fillSpaceBySpaceLevel(Space space);

    // 将查询请求转化为QueryWrapper对象
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest request);

    // 分页获取SpaceVO对象
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    // 创建空间
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    // 校验空间权限
    void checkSpaceAuth(Space space, User loginUser);
}
