package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureEditByBatchRequest {
    private static final long serialVersionUID = 1L;
    /**
     * 要批量修改的图片id
     */
    private List<Long> pictureIdList;
    /**
     * 修改图片所在空间id
     */
    private Long spaceId;
    /**
     * 要修改的图片种类
     */
    private String category;
    /**
     * 要修改的图片标签
     */
    private List<String> tags;
    /**
     * 要修改的图片名称规则
     */
    private String nameRule;
}
