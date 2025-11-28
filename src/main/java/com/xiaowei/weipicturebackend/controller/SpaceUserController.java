package com.xiaowei.weipicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.DeleteRequest;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.auth.anotation.SaSpaceCheckPermission;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.model.vo.SpaceUserVO;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 空间用户关联 前端控制器
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-27
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @PostMapping("/add")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserService.addSpaceUser(spaceUserAddRequest,request));
    }
    /**
     * 根据id删除空间（仅管理员和本人）
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUserById(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        //检查是否存在数据
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        if (oldSpaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //判断是否为空间成员
        if(oldSpaceUser.getUserId()==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"该空间中没有此成员");
        }
        //删除
        boolean remove = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.PARAMS_ERROR, "删除失败");
        return ResultUtils.success(true);
    }
    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUserById(@RequestBody SpaceUserQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        //必须传入spaceId和userId
        Long spaceId = queryRequest.getSpaceId();
        Long userId = queryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasNull(spaceId, userId), ErrorCode.PARAMS_ERROR);
        //查询数据库
        SpaceUser spaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId).eq(SpaceUser::getUserId, userId).one();
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        return ResultUtils.success(spaceUser);
    }
    /**
     * 分页获取空间信息
     * @param queryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUser>> listSpaceUserByPage(@RequestBody SpaceUserQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(queryRequest));
        return ResultUtils.success(spaceUserList);
    }

    @PostMapping("/edit")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
        public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null, ErrorCode.PARAMS_ERROR);
        //获取编辑信息
        SpaceUser spaceUser=new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, spaceUser);
        //校验空间信息
        spaceUserService.validSpaceUser(spaceUser, false);
        //查询数据库是否存在需要编辑的空间
        SpaceUser oldSpaceUser = spaceUserService.getById(spaceUser.getId());
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        //判断是否为空间成员
        if(oldSpaceUser.getUserId()==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"该空间中没有此成员");
        }
        //如果要更新的权限和原来的一致，则不需要更新
        if(oldSpaceUser.getSpaceRole().equals(spaceUserEditRequest.getSpaceRole()))
        {
            return ResultUtils.success(true);
        }
        //更新数据库
        boolean update = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"编辑失败");
        return ResultUtils.success(true);
    }

    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        List<SpaceUser> spaceUserList = spaceUserService.lambdaQuery().eq(SpaceUser::getUserId, userId).list();
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

}
