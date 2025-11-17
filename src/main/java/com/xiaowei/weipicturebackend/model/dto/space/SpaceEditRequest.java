package com.xiaowei.weipicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑自己空间需要的请求类
 */
@Data
public class SpaceEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

}
