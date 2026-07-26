package com.zsh.zshpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.DeleteRequest;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.dto.user.*;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.LoginUserVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 用户注册
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if(userRegisterRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userService.userRegister(
                userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword()
        );
        return ResultUtils.success(userId);
    }

    // 用户登录
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if(userLoginRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(
                userLoginRequest.getUserAccount(),
                userLoginRequest.getUserPassword(),
                request
        );
        return ResultUtils.success(loginUserVO);
    }

    // 获取当前登录的用户
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUserVO(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
    }

    // 用户退出登录
    // 用POST是因为该操作会移除当前Session的用户登录态，修改了服务器状态，本质上是一个写操作
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    // 创建用户
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        if(userAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user=new User();
        BeanUtils.copyProperties(userAddRequest,user);
        user.setUserPassword(userService.getEncryptPassword(UserConstant.DEFAULT_PASSWORD));
        // 插入数据库
        boolean result = userService.save(user);
        if(!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"创建用户失败");
        }
        return ResultUtils.success(user.getId());
    }

    // 根据id获取用户信息（仅管理员）
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        if(id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"id<=0");
        }
        User user = userService.getById(id);
        if(user==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(user);
    }

    // 根据id获取脱敏后的用户信息
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        User user = getUserById(id).getData();
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    // 删除用户
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        if(deleteRequest==null || deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean res = userService.removeById(deleteRequest.getId());
        if(!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"删除用户失败");
        }
        return ResultUtils.success(true);
    }

    // 更新用户
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        if(userUpdateRequest==null || userUpdateRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user=new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        boolean res = userService.updateById(user);// 它只会更新你传入的实体对象中不为 null 的字段，而对 null 值的字段则保持数据库原值不变
        if(!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"更新用户失败");
        }
        return ResultUtils.success(true);
    }

    // 分页查询脱敏后的用户列表（仅管理员）
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest){
        if(userQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        QueryWrapper<User> qw = userService.getQueryWrapper(userQueryRequest);
        Page<User> userPage = userService.page(new Page<>(current, pageSize), qw);

        Page<UserVO> userVOPage=new Page<>(current,pageSize,userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
