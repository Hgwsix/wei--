package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureSearchPictureByColorRequest implements Serializable  {
    private static final long serialVersionUID = 1L;
    /**
     * 颜色
     */
    private String picColor;
    /**
     * 空间id
     */
    private Long spaceId;

}
