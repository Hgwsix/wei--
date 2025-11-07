package com.xiaowei.weipicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PictureReviewStatusEnum {
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;
    private final Integer value;

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getPictureReviewStatusEnum(Integer value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getValue().equals(value))
                return pictureReviewStatusEnum;
        }
        return null;
    }
}
