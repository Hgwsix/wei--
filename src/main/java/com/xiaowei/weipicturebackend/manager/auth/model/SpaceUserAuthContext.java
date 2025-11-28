package com.xiaowei.weipicturebackend.manager.auth.model;

import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import lombok.Data;

/**
 * 表示用户在特定区间内的授权上下文，包括关联的图片、空间和用户信息
 */
@Data
public class SpaceUserAuthContext {
    /**
     * 临时参数，不同请求对应的id可能不同
     */
    private Long id;
    /**
     * 图片id
     */
    private Long pictureId;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 空间用户id
     */
    private Long spaceUserId;
    /**
     * 图片
     */
    private Picture picture;
    /**
     * 空间用户信息
     */
    private SpaceUser spaceUser;
    /**
     * 空间
     */
    private Space space;
}
