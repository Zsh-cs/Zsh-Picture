package com.zsh.zshpicturebackend.aop;

import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.enums.UserRoleEnum;
import com.zsh.zshpicturebackend.model.vo.LoginUserVO;
import com.zsh.zshpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验拦截器
 */
@Aspect
@Component
public class AuthInterceptor {

    @Autowired
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object handleAround(ProceedingJoinPoint pjp, AuthCheck authCheck) throws Throwable {

        // 获取当前登录用户，如果未登录会直接抛异常（在getLoginUser()方法里写了）
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        LoginUserVO loginUser = userService.getLoginUser(request);

        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(authCheck.mustRole());
        // 如果不需要权限，直接放行
        if (mustRoleEnum == null) {
            return pjp.proceed();
        }
        // 以下代码：用户必须有对应权限才能通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());// 用户现在具有的权限
        if(userRoleEnum ==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果要求必须有管理员权限，但用户现在没有管理员权限，则不能放行
        if (mustRoleEnum.equals(UserRoleEnum.ADMIN) && !userRoleEnum.equals(UserRoleEnum.ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return pjp.proceed();
    }
}
