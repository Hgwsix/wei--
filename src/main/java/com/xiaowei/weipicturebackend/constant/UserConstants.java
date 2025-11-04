package com.xiaowei.weipicturebackend.constant;


import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;

public final class UserConstants {
    /**
     * 用户登录状态
     */
    public static final String USER_LOGIN_STATE="user_login";

    /**
     * 用户默认角色User
     */
    public static final String DEFAULT_ROLE="user";

    /**
     * 管理员身份
     */
    public static final String ADMIN_ROLE="admin";

    /**
     * 私有构造函数，以防被实例化
     */
    private UserConstants() {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不允许实例化常量类:UserConstants");
    }
}
