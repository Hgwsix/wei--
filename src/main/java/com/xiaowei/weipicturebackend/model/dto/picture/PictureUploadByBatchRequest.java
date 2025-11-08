package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 每次批量抓取图片的数量（默认10张）
     */
    private Integer count=10;
    /**
     * 名称前缀（用来给抓取的图片命名）
     */
    private String namePrefix;
}
