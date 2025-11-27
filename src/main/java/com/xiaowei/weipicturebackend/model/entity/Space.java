package com.xiaowei.weipicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 空间
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-15
 */
@Getter
@Setter
@ToString
@TableName("space")
public class Space implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    @TableField("spaceName")
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @TableField("spaceLevel")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    @TableField("maxSize")
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    @TableField("maxCount")
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    @TableField("totalSize")
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    @TableField("totalCount")
    private Long totalCount;

    /**
     * 创建用户 id
     */
    @TableField("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    @TableField("editTime")
    private LocalDateTime editTime;

    /**
     * 更新时间
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField("isDelete")
    @TableLogic
    private Byte isDelete;
    /**
     * 空间类型
     */
    @TableField("spaceType")
    private Integer spaceType;
}
