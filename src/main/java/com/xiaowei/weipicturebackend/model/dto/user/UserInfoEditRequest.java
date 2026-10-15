package com.xiaowei.weipicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserInfoEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户Id
     */
    private Long userId;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户简介
     */
    private String userProfile;



}
