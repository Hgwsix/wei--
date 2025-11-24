package com.xiaowei.weipicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间容量使用排行的请求类
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 获取前N个空间
     */
    private Integer topN=10;

    private static final long serialVersionUID = 1L;
}
