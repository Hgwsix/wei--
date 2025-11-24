package com.xiaowei.weipicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.*;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用分析的方法
     *
     * @param request   空间使用分析请求参数对象，包含分析所需的各种条件
     * @param loginUser 当前登录用户对象，用于权限验证和用户相关数据获取
     * @return SpaceUsageAnalyzeResponse 空间使用分析结果对象，包含分析后的空间使用数据
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest request, User loginUser);

    /**
     * 获取空间分类分析结果的方法
     *
     * @param request   空间分类分析请求参数对象，包含分析所需的各项条件
     * @param loginUser 当前登录用户信息，用于权限验证和数据过滤
     * @return 返回空间分类分析结果列表，每个元素包含对应分类的详细分析数据
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest request, User loginUser);

    /**
     * 获取空间标签分析结果的方法
     *
     * @param request   空间标签分析请求参数，包含分析所需的各项条件
     * @param loginUser 当前登录用户信息，用于权限验证和个性化分析
     * @return 返回空间标签分析结果列表，每个元素包含一个空间的标签分析数据
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest request, User loginUser);

/**
 * 获取空间大小分析结果的方法
 *
 * @param spaceSizeAnalyzeRequest 空间大小分析请求对象，包含分析所需的参数
 * @param loginUser 当前登录用户对象，用于验证用户权限和获取用户相关信息
 * @return 返回空间大小分析响应对象的列表，包含分析结果数据
 */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);
/**
 * 获取空间用户分析数据的方法
 *
 * @param spaceUserAnalyzeRequest 空间大小分析请求参数，包含需要分析的空间相关信息
 * @param loginUser 当前登录用户信息，用于权限验证和用户相关数据获取
 * @return 返回空间用户分析结果的列表，每个元素包含一个用户的分析数据
 */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);
/**
 * 获取空间排名分析数据
 *
 * @param request 空间排名分析请求参数，包含查询条件等信息
 * @param loginUser 当前登录用户信息，用于权限验证和数据过滤
 * @return 返回空间排名分析结果列表，每个元素包含一个空间的详细分析数据
 */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest request, User loginUser);
}
