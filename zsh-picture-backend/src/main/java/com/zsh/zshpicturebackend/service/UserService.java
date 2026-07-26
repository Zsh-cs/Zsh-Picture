package com.zsh.zshpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zsh.zshpicturebackend.model.dto.user.UserQueryRequest;
import com.zsh.zshpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zsh.zshpicturebackend.model.vo.LoginUserVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author asus
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2026-07-19 17:01:43
 */
public interface UserService extends IService<User> {

    // 用户注册
    long userRegister(String userAccount, String userPassword, String checkPassword);

    // 用户登录，返回脱敏后的用户信息
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    // 获取当前登录的用户
    User getLoginUser(HttpServletRequest request);

    // 用户退出登录
    boolean userLogout(HttpServletRequest request);

    // 获得脱敏后的用户信息
    UserVO getUserVO(User user);

    // 获得脱敏后的用户信息列表
    List<UserVO> getUserVOList(List<User> users);

    // 获取加密后的密码
    String getEncryptPassword(String password);

    // 将查询请求转换为QueryWrapper对象
    QueryWrapper<User> getQueryWrapper(UserQueryRequest request);

    // 对用户信息进行脱敏
    LoginUserVO getLoginUserVO(User user);
}
