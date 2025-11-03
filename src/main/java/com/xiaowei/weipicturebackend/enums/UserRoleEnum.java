package com.xiaowei.weipicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;
    private final String value;

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getUserRoleEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.getValue().equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

}
