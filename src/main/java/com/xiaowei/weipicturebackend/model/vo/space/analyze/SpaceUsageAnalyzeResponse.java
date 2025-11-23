package com.xiaowei.weipicturebackend.model.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间资源使用分析响应类
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 已使用空间大小
     */
    private Long usedSize;
    /**
     * 最大空间容量
     */
    private Long maxSize;
    /**
     * 空间使用比例
     */
    private Double sizeUsageRadio;
    /**
     * 已使用图片数量
     */
    private Long usedCount;
    /**
     * 最大图片数量
     */
    private Long maxCount;
    /**
     * 图片数量使用比例
     */
    private Double countUsageRadio;

}
