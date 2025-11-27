package com.xiaowei.weipicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpaceUserRoleEnums {
    VIEWER("浏览者","viewer"),
    EDITOR("编辑者","editor"),
    ADMIN("管理员","admin");

    private final String text;
    private final String value;

    public static SpaceUserRoleEnums getSpaceUserRoleEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceUserRoleEnums spaceUserRoleEnums : SpaceUserRoleEnums.values()) {
            if (spaceUserRoleEnums.getValue().equals(value))
                return spaceUserRoleEnums;
        }
        return null;
    }

}
