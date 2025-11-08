package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片id（用于修改）
     */
    private Long id;

    /**
     * 图片url
     */
    private String fileUrl;

    /**
     * 图片名称前缀
     */
    private String picName;
}
