package com.xiaowei.weipicturebackend.model.dto.user;

import java.io.Serializable;

public class UserRegisterRequest implements Serializable {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 邀请码
     */
    private String inviteCode;

    private static final long serialVersionUID = 1L;
}
