package com.xiaowei.weipicturebackend.manager.auth.model;

import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;

public class SpaceUserPermissionConstant {
    
    private SpaceUserPermissionConstant() {
        // 防止实例化
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不允许实例化常量类：SpaceUserPermissionConstant");
    }
    
    /**
     * 空间用户管理权限
     */
    public static final String SPACE_USER_MANAGE = "spaceUser:manage";

    /**
     * 图片查看权限
     */
    public static final String PICTURE_VIEW = "picture:view";

    /**
     * 图片上传权限
     */
    public static final String PICTURE_UPLOAD = "picture:upload";

    /**
     * 图片编辑权限
     */
    public static final String PICTURE_EDIT = "picture:edit";

    /**
     * 图片删除权限
     */
    public static final String PICTURE_DELETE = "picture:delete";
}