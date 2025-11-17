package com.xiaowei.weipicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 中文
     */
    private  String text;
    /**
     * 值
     */
    private  Integer value;
    /**
     * 空间存取的图片的最大数量
     */
    private  long maxCount;
    /**
     * 空间存取的图片的最大容量
     */
    private  long maxSize;
}
