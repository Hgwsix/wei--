package com.xiaowei.weipicturebackend.controller;

import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.xiaowei.weipicturebackend.service.SpaceAnalyzeService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/space")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> spaceTagAnalyze = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyze);
    }
}
