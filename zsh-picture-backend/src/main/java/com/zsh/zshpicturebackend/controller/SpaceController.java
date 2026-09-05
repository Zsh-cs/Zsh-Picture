package com.zsh.zshpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.DeleteRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceAddRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceEditRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.SpaceLevelEnum;
import com.zsh.zshpicturebackend.model.vo.space.SpaceLevel;
import com.zsh.zshpicturebackend.model.vo.space.SpaceVO;
import com.zsh.zshpicturebackend.service.SpaceService;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Autowired
    private SpaceService spaceService;
    @Autowired
    private UserService userService;

    // 删除空间
    // todo 删除空间后，关联删除空间内的所有图片
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        Space space = spaceService.getById(id);
        // 判断空间是否存在
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 只有空间创建人或管理员可以删除空间
        spaceService.checkSpaceAuth(space, loginUser);

        boolean res = spaceService.removeById(id);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 更新空间：仅管理员可用，允许更新空间级别
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将DTO转换成实体
        Space newSpace = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, newSpace);
        // 校验空间
        spaceService.verifySpace(newSpace);
        // 判断id对应空间是否存在
        Space oldSpace = spaceService.getById(newSpace.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 根据空间级别自动填充限额数据
        spaceService.fillSpaceBySpaceLevel(newSpace);
        // 操作数据库
        boolean res = spaceService.updateById(newSpace);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 编辑空间：允许空间创建人使用，但要限制可编辑的字段，不能编辑空间级别
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest,
                                           HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将DTO转换成实体
        Space newSpace = new Space();
        BeanUtils.copyProperties(spaceEditRequest, newSpace);
        newSpace.setEditTime(new Date());// 注意要设置编辑时间
        // 校验空间
        spaceService.verifySpace(newSpace);
        // 判断id对应空间是否存在
        Space oldSpace = spaceService.getById(newSpace.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 只有空间创建人或管理员可以编辑空间
        User loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(oldSpace, loginUser);
        // 空间创建人不能编辑空间级别
        if (oldSpace.getUserId().equals(loginUser.getId()) && !oldSpace.getSpaceLevel().equals(newSpace.getSpaceLevel())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间创建人不能编辑空间级别");
        }
        // 根据空间级别自动填充限额数据
        spaceService.fillSpaceBySpaceLevel(newSpace);
        // 操作数据库
        boolean res = spaceService.updateById(newSpace);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 根据id获取空间（仅管理员可用）
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    // 根据id获取SpaceVO对象
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.obj2vo(space);
        return ResultUtils.success(spaceVO);
    }

    // 分页获取空间列表（仅管理员可用）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        QueryWrapper<Space> qw = spaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), qw);
        return ResultUtils.success(spacePage);
    }

    // 分页获取SpaceVO列表
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "一页展示条数过多");
        QueryWrapper<Space> qw = spaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), qw);
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage));
    }

    // 创建空间
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    // 获取空间级别列表，便于前端展示
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel(){
        List<SpaceLevel> spaceLevelList=new ArrayList<>();
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            SpaceLevel spaceLevel=new SpaceLevel(
                    spaceLevelEnum.getText(),
                    spaceLevelEnum.getValue(),
                    spaceLevelEnum.getMaxCount(),
                    spaceLevelEnum.getMaxSize()
            );
            spaceLevelList.add(spaceLevel);
        }
        return ResultUtils.success(spaceLevelList);
    }

}
