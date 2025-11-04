package com.xiaowei.weipicturebackend.model.dto.user;

import lombok.Data;
import lombok.Getter;

@Data
public class UserLoginRequest {

    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
}
