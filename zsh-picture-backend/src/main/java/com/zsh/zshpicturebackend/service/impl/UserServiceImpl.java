package com.zsh.zshpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.dto.user.UserQueryRequest;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.UserRoleEnum;
import com.zsh.zshpicturebackend.model.vo.LoginUserVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;
import com.zsh.zshpicturebackend.service.UserService;
import com.zsh.zshpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author asus
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2026-07-19 17:01:43
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Autowired
    private UserMapper userMapper;

    // 用户注册
    @Override
    @Transactional
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在空参数");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号名过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!checkPassword.equals(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户两次输入的密码不一致");
        }

        // 2.检查用户账户是否和数据库中已有的账户重复
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUserAccount, userAccount);
        Long count = userMapper.selectCount(lqw);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号重复");
        }

        // 3.密码一定要加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4.插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败，数据库错误");
        }
        return user.getId();
    }

    // 用户登录，返回脱敏后的用户信息
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在空参数");
        }

        // 2.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 3.查询数据库，比对账号和密码
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUserAccount, userAccount);
        lqw.eq(User::getUserPassword, encryptPassword);
        User user = userMapper.selectOne(lqw);
        if (user == null) {
            log.info("user login failed, userAccount doesn't exist or userPassword is wrong.");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不存在或密码错误");
        }

        // 4.保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    // 获取当前登录的用户
    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中再查询一遍，避免缓存和数据库不一致导致的问题
        currentUser = this.getById(currentUser.getId());
        if(currentUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"该用户已注销");
        }
        return getLoginUserVO(currentUser);
    }

    // 用户退出登录
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录则无法执行退出登录操作");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    // 获得脱敏后的用户信息
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    // 获得脱敏后的用户信息列表
    @Override
    public List<UserVO> getUserVOList(List<User> users) {
        if (CollUtil.isEmpty(users)) {
            return new ArrayList<>();
        }
        return users.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    // 获取加密后的密码
    @Override
    public String getEncryptPassword(String password) {
        // 加盐，混淆密码
        final String SALT = "zsh";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    // 将查询请求转换为QueryWrapper对象
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest request) {
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }

        Long id = request.getId();
        String userName = request.getUserName();
        String userAccount = request.getUserAccount();
        String userProfile = request.getUserProfile();
        String userRole = request.getUserRole();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();

        QueryWrapper<User> qw=new QueryWrapper<>();
        qw.eq(id!=null,"id",id)
                .like(StrUtil.isNotBlank(userName),"userName",userName)
                .like(StrUtil.isNotBlank(userAccount),"userAccount",userAccount)
                .like(StrUtil.isNotBlank(userProfile),"userProfile",userProfile)
                .eq(StrUtil.isNotBlank(userRole),"userRole",userRole)
                .orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return qw;
    }

    // 对用户信息进行脱敏
    private LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }


}




