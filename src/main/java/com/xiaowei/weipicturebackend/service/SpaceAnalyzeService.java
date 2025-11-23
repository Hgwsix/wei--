package com.xiaowei.weipicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

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
 * @param request 空间分类分析请求参数对象，包含分析所需的各项条件
 * @param loginUser 当前登录用户信息，用于权限验证和数据过滤
 * @return 返回空间分类分析结果列表，每个元素包含对应分类的详细分析数据
 */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest request, User loginUser);

/**
 * 获取空间标签分析结果的方法
 *
 * @param request 空间标签分析请求参数，包含分析所需的各项条件
 * @param loginUser 当前登录用户信息，用于权限验证和个性化分析
 * @return 返回空间标签分析结果列表，每个元素包含一个空间的标签分析数据
 */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest request, User loginUser);
}
