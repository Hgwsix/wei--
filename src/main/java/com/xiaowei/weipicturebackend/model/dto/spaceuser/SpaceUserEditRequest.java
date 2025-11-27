package com.xiaowei.weipicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    /**
     * 用户在空间中的角色 viewer/editor/admin
     */
    private String spaceRole;
}
