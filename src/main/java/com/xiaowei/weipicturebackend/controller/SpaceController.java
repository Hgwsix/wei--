package com.xiaowei.weipicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.annotation.AuthCheck;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.DeleteRequest;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.enums.SpaceLevelEnums;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.auth.SpaceUserAuthManager;
import com.xiaowei.weipicturebackend.manager.auth.anotation.SaSpaceCheckPermission;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xiaowei.weipicturebackend.model.dto.space.*;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.SpaceVO;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 空间 前端控制器
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-15
 */
@RestController
@RequestMapping("/space")
public class SpaceController {


   @Resource
   private UserService userService;

   @Resource
   private SpaceService spaceService;

   @Resource
   private PictureService pictureService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @PostMapping("/add")
   @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
   public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request){
       ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
       return ResultUtils.success(spaceService.addSpace(spaceAddRequest,request,null));
   }
    /**
     * 根据id删除空间（仅管理员和本人）
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")

    public BaseResponse<Boolean> deleteSpaceById(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        //检查是否存在数据
        Space oldSpace = spaceService.getById(id);
        if (oldSpace == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //权限校验（只要管理员和本人可以删除空间）
        spaceService.checkSpaceAuth(oldSpace, loginUser);
        //删除空间
        boolean remove = spaceService.removeById(id);
        if (!remove) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        //删除空间后，与之关联的图片也应该删除
        //删除前确保空间中有图片
        boolean exists = pictureService.lambdaQuery().eq(Picture::getSpaceId, id).exists();
        if(exists){
            LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>()
                    .eq(Picture::getSpaceId, id);
            boolean removePicture = pictureService.remove(queryWrapper);
            ThrowUtils.throwIf(!removePicture, ErrorCode.OPERATION_ERROR, "删除关联空间的图片失败");
        }

        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        Space Space = new Space();
        BeanUtil.copyProperties(updateRequest, Space);
        //自动填充限额
        spaceService.fillSpaceLevel(Space);
        //校验空间数据
        spaceService.validSpace(Space, false);
        //检查是否存在数据
        Space oldSpace = spaceService.getById(updateRequest.getId());
        if (oldSpace == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //更新空间
        boolean updateResult = spaceService.updateById(Space);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 分页获取空间信息
     * @param queryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = queryRequest.getCurrent();
        int pageSize = queryRequest.getPageSize();
        Page<Space> spacePage = spaceService.page(Page.of(current, pageSize), spaceService.getQueryWrapper(queryRequest));
        return ResultUtils.success(spacePage);
    }    /**
     * 分页获取空间信息（封装类）
     * @param queryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(spaceService.listSpaceVOByPage(queryRequest));
    }
    /**
     * 根据id获取空间封装类
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //将用户脱敏后信息封装进空间封装类
        SpaceVO SpaceVO = spaceService.getSpaceVO(space);
        Long userId = space.getUserId();
        User byId = userService.getById(userId);
        if(byId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        SpaceVO.setUser(userService.getUserVO(byId));

        User loginUser = userService.getLoginUser(request);
        // 获取权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        SpaceVO.setPermissionList(permissionList);
        return ResultUtils.success(SpaceVO);

    }
    /**
     * 根据id获取空间信息（管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Space Space = spaceService.getById(id);
        ThrowUtils.throwIf(Space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(Space);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest SpaceEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(SpaceEditRequest == null, ErrorCode.PARAMS_ERROR);
        //1.获取编辑信息
        Space Space=new Space();
        BeanUtil.copyProperties(SpaceEditRequest, Space);
        //校验空间信息
        spaceService.validSpace(Space, false);
        //2.查询数据库是否存在需要编辑的空间
        Space oldSpace = spaceService.getById(Space.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        //3.校验信息，只有本人和管理员可以编辑
        User loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(oldSpace, loginUser);
        //4.设置编辑时间
        Space.setEditTime(LocalDateTime.now());
        //5.更新数据库
        boolean update = spaceService.updateById(Space);
        ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"编辑失败");
        return ResultUtils.success(true);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> collect = Arrays.stream(SpaceLevelEnums.values()).map(spaceLevelEnum -> new SpaceLevel(
                spaceLevelEnum.getText(),
                spaceLevelEnum.getValue(),
                spaceLevelEnum.getMaxCount(),
                spaceLevelEnum.getMaxSize()
        )).collect(Collectors.toList());
        return ResultUtils.success(collect);
    }

}
