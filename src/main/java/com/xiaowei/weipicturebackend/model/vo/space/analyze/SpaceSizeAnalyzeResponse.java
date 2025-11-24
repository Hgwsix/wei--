package com.xiaowei.weipicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片大小分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {
    /**
     * 图片空间范围
     */
    private String sizeRange;
    /**
     * 对应范围的图片数量
     */
    private Long count;
    private static final long serialVersionUID = 1L;
}
