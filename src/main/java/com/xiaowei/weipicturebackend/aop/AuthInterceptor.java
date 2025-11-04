package com.xiaowei.weipicturebackend.aop;

import cn.hutool.core.util.StrUtil;
import com.xiaowei.weipicturebackend.annotation.AuthCheck;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.enums.UserRoleEnum;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取登入用户
        ServletRequestAttributes attributes= (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
        }
        User loginUser = userService.getLoginUser(request);
        //必要角色为空，放行
        UserRoleEnum mustRoleEnum = UserRoleEnum.getUserRoleEnumByValue(authCheck.mustRole());
        if(mustRoleEnum==null){
            return joinPoint.proceed();
        }
        //必要角色不为空，判断用户角色是否包含必要角色，不包含抛出异常

        //没有权限，拒绝
        UserRoleEnum userRoleEnum =UserRoleEnum.getUserRoleEnumByValue(loginUser.getUserRole());
        if(userRoleEnum==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限访问");
        }
        //需要管理员，但登入用户不是管理员
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限访问");

        }
        //通过权限校验，放行
        return joinPoint.proceed();
    }
}
