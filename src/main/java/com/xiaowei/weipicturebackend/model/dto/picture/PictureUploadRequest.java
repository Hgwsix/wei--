package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片id（用于修改）
     */
    private Long id;

}
