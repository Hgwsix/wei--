package com.xiaowei.weipicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceAddRequest;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * <p>
 * 空间 服务类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-15
 */
public interface SpaceService extends IService<Space> {


    /**
     * 根据Space对象获取对应的SpaceVO对象
     * SpaceVO可能是用于视图展示的对象，包含了Space的部分或全部属性
     *
     * @param Space 包含空间信息的实体对象
     * @return 返回一个用于视图展示的SpaceVO对象
     */
    SpaceVO getSpaceVO(Space Space);

    /**
     * 根据空间列表获取对应的视图对象列表
     *
     * @param SpaceList 空间实体对象列表
     * @return 返回空间视图对象列表，用于前端展示
     */
    List<SpaceVO> getSpaceVOList(List<Space> SpaceList);

    /**
     * 根据查询请求参数构建Space实体的查询条件封装器
     *
     * @param
     * @return Wrapper<Space> 返回一个Space实体的查询条件封装器，用于数据库查询操作
     */
    Wrapper<Space> getQueryWrapper(SpaceQueryRequest queryRequest);

    /**
     * 分页查询空间视图对象列表
     *
     * @return 返回一个包含SpaceVO对象的分页结果，包含数据列表和分页信息
     */
    Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest queryRequest);

    /**
     * @param spaceAddRequest 包含空间编辑信息的请求对象，用于添加新空间
     * @return 返回一个Boolean值，表示空间添加操作是否成功
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);

    /**
     * 填充空间等级的方法
     *
     * @param space 需要填充等级的Space对象
     */
    void fillSpaceLevel(Space space);

    /**
     * 校验数据，创建时校验和更新时校验逻辑是不一样的
     *
     * @param space 需要验证的空间对象
     * @param add   布尔值，决定是否添加空间
     */
    void validSpace(Space space, boolean add);

    /**
     * 检查用户在指定空间中的权限
     *
     * @param space     需要检查权限的空间对象
     * @param loginUser 当前登录用户对象
     *                  void 表示此方法不返回任何值
     */
    void checkSpaceAuth(Space space, User loginUser);
}
