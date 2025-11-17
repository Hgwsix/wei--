package com.xiaowei.weipicturebackend.model.dto.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUpdateRequest implements Serializable {
    private static final long serializeVersionUID=1L;
    /**
     * 图片id
     */
    private Long id;
    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     * 使用数组，方便前端接收
     */
    private List<String> tags;


}
