package com.xiaowei.weipicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户角色
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
