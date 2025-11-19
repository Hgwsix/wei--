package com.xiaowei.weipicturebackend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

@Data
/**
 * 用于封装上传图片后，经过万象数据处理后得到的图片信息
 */
public class UploadPictureResult implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片 url
     */
    private String url;
    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 图片主色调
     */
    private String picColor;


}
