package com.xiaowei.weipicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpaceLevelEnums {
    COMMON("普通版",0,100,100L*1024*1024),
    PROFESSIONAL("专业版",1,1000,1000L*1024*1024),
    FLAGSHIP("旗舰版",2,10000,10000L*1024*1024);
    private final String text;
    private final Integer value;
    /**
     * 空间存取的图片的最大数量
     */
    private final long maxCount;
    /**
     * 空间存取的图片的最大容量
     */
    private final long maxSize;

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static SpaceLevelEnums getSpaceLevelEnumByValue(Integer value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceLevelEnums spaceLevelEnums : SpaceLevelEnums.values()) {
            if (spaceLevelEnums.getValue().equals(value)) {
                return spaceLevelEnums;
            }
        }
        return null;
    }
}
