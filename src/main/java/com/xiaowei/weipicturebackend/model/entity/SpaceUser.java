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
 * 空间用户关联
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-27
 */
@Getter
@Setter
@ToString
@TableName("space_user")
public class SpaceUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 空间 id
     */
    @TableField("spaceId")
    private Long spaceId;

    /**
     * 用户 id
     */
    @TableField("userId")
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    @TableField("spaceRole")
    private String spaceRole;

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
}
