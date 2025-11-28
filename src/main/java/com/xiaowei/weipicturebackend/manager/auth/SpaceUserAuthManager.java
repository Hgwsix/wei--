package com.xiaowei.weipicturebackend.manager.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;
    static {
        // 从配置文件中加载权限配置
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG= BeanUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取对应的权限
     */
    public static List<String> getPermissionsByRole(String roleKey) {
        if(StrUtil.isBlank(roleKey)) {
            return new ArrayList<>();
        }

        SpaceUserRole spaceUserRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(roleKey))
                .findFirst()
                .orElse(null);
        if(spaceUserRole == null) {
          return List.of();
        }
        return spaceUserRole.getPermissions();
    }

}
