package com.xiaowei.weipicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpaceTypeEnums {
    PRIVATE("私人空间",0),
    TEAM("团队空间",1);

    private final String text;
    private final Integer value;

    public static SpaceTypeEnums getSpaceTypeEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnums spaceTypeEnums : SpaceTypeEnums.values()) {
            if (spaceTypeEnums.getValue().equals(value))
                return spaceTypeEnums;
        }
        return null;
    }
}
