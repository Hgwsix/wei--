package com.xiaowei.weipicturebackend.model.vo;

import com.xiaowei.weipicturebackend.model.vo.space.SpaceVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserVO implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     *用户id
     */
    private Long userId;
    /**
     * 用户角色
     */
    private String spaceRole;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新时间
     */
    private Long updateTime;
    /**
     * 用户信息
     */
    private UserVO user;
    /**
     * 空间信息
     */
    private SpaceVO space;

    private static final long serialVersionUID = 1L;
}
