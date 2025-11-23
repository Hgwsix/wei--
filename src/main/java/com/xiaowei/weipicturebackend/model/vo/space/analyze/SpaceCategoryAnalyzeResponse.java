package com.xiaowei.weipicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片所属的分类
     */
    private String category;
    /**
     * 该分类所有的图片数
     */
    private Long count;
    /**
     * 该分类所有图片的大小
     */
    private Long totalSize;
}
