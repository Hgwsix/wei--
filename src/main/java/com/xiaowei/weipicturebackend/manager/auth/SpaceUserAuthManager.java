package com.xiaowei.weipicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaowei.weipicturebackend.enums.SpaceTypeEnums;
import com.xiaowei.weipicturebackend.enums.SpaceUserRoleEnums;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserRole;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        // 从配置文件中加载权限配置
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
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

    /**
     * 获取用户在空间中的权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        //如果用户没登入，则无权限访问空间或者使用空间功能
        if (loginUser == null) {
            return new ArrayList<>();
        }
        //管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceUserRoleEnums.ADMIN.getValue());
        if (space == null) {
            //公共图库
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnums spaceTypeEnumByValue = SpaceTypeEnums.getSpaceTypeEnumByValue(spaceType);
        ThrowUtils.throwIf(spaceTypeEnumByValue == null, ErrorCode.NOT_FOUND_ERROR, "空间类型不存在");
        switch (spaceTypeEnumByValue) {
            case PRIVATE: {
                //私人空间，仅空间管理员和管理员有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }
                return new ArrayList<>();
            }
            case TEAM: {
                //团队空间，查询空间用户信息表获取对应权限
                Long spaceId = space.getId();
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else return getPermissionsByRole(spaceUser.getSpaceRole());
            }
            default:
                return new ArrayList<>();
        }
    }
}
