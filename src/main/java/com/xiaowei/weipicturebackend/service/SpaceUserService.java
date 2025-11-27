package com.xiaowei.weipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.SpaceUserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * <p>
 * 空间用户关联 服务类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-27
 */
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 根据SpaceUser对象获取对应的SpaceUserVO对象
     * SpaceUserVO可能是用于视图展示的对象，包含了SpaceUser的部分或全部属性
     *
     * @param spaceUser 包含空间信息的实体对象
     * @return 返回一个用于视图展示的SpaceUserVO对象
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 根据空间列表获取对应的视图对象列表
     *
     * @param spaceUserList 空间实体对象列表
     * @return 返回空间视图对象列表，用于前端展示
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 根据查询请求参数构建SpaceUser实体的查询条件封装器
     *
     * @param
     * @return Wrapper<SpaceUser> 返回一个SpaceUser实体的查询条件封装器，用于数据库查询操作
     */
    Wrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest queryRequest);

    /**
     * @param spaceUserAddRequest 包含空间编辑信息的请求对象，用于添加新空间
     * @return 返回一个Boolean值，表示空间添加操作是否成功
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request);

    /**
     * 校验数据，创建时校验和更新时校验逻辑是不一样的
     *
     * @param spaceUser 需要验证的空间对象
     * @param add       布尔值，决定是否添加空间
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
}

