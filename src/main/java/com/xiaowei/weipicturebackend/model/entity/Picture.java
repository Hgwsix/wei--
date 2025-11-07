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
 * 图片
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-05
 */
@Getter
@Setter
@ToString
@TableName("picture")
public class Picture implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    @TableField("url")
    private String url;

    /**
     * 图片名称
     */
    @TableField("name")
    private String name;

    /**
     * 简介
     */
    @TableField("introduction")
    private String introduction;

    /**
     * 分类
     */
    @TableField("category")
    private String category;

    /**
     * 标签（JSON 数组）
     */
    @TableField("tags")
    private String tags;

    /**
     * 图片体积
     */
    @TableField("picSize")
    private Long picSize;

    /**
     * 图片宽度
     */
    @TableField("picWidth")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @TableField("picHeight")
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @TableField("picScale")
    private Double picScale;

    /**
     * 图片格式
     */
    @TableField("picFormat")
    private String picFormat;

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
    private Byte isDelete;
    /**
     * 审核状态
     */
    @TableField("reviewStatus")
    private Integer reviewStatus;
    /**
     * 审核信息
     */
    @TableField("reviewMessage")
    private String reviewMessage;
    /**
     * 审核人 id
     */
    @TableField("reviewerId")
    private Long reviewerId;
    /**
     * 审核时间
     */
    @TableField("reviewTime")
    private LocalDateTime reviewTime;
}
