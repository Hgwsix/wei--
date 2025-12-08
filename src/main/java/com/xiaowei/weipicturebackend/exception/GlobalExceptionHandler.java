package com.xiaowei.weipicturebackend.exception;


import cn.dev33.satoken.exception.NotLoginException;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.NoPermissionException;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "未登录");
    }

    @ExceptionHandler(NoPermissionException.class)
    public BaseResponse<?> noPermissionExceptionHandler(NoPermissionException e) {
        log.error("NoPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "无权限");
    }
}
