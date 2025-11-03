package com.xiaowei.weipicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-03
 */
@Getter
@Setter
@ToString
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    @TableField("userAccount")
    private String userAccount;

    /**
     * 密码
     */
    @TableField("userPassword")
    private String userPassword;

    /**
     * 用户昵称
     */
    @TableField("userName")
    private String userName;

    /**
     * 用户头像
     */
    @TableField("userAvatar")
    private String userAvatar;

    /**
     * 用户简介
     */
    @TableField("userProfile")
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    @TableField("userRole")
    private String userRole;

    /**
     * 编辑时间
     */
    @TableField("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField("isDelete")
    private Byte isDelete;

    /**
     * 会员过期时间
     */
    @TableField("vipExpireTime")
    private LocalDateTime vipExpireTime;

    /**
     * 会员兑换码
     */
    @TableField("vipCode")
    private String vipCode;

    /**
     * 会员编号
     */
    @TableField("vipNumber")
    private Long vipNumber;

    /**
     * 分享码
     */
    @TableField("shareCode")
    private String shareCode;

    /**
     * 邀请用户 id
     */
    @TableField("inviteUser")
    private Long inviteUser;
}
