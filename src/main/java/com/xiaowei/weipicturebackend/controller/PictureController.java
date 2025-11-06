package com.xiaowei.weipicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.annotation.AuthCheck;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.DeleteRequest;
import com.xiaowei.weipicturebackend.common.PageRequest;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureQueryRequest;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureUpdateRequest;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureUploadRequest;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.PictureVO;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

/**
 * <p>
 * 图片 前端控制器
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-05
 */
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片（可以重复上传），仅管理员
     *
     * @param multipartFile
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.uploadPicture(multipartFile, uploadPictureRequest, request);
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    /**
     * 根据id删除图片（仅管理员和本人）
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePictureById(@RequestBody DeleteRequest deleteRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        //权限校验（只要管理员和本人可以删除图片）
        if (!loginUser.getId().equals(id) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //检查是否存在数据
        Picture oldPicture = pictureService.getById(id);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //删除图片
        boolean remove = pictureService.removeById(id);
        if (!remove) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateRequest, picture);
        //将tags数组转json字符串
        picture.setTags(JSONUtil.toJsonStr(updateRequest.getTags()));
        //检查是否存在数据
        Picture oldPicture = pictureService.getById(updateRequest.getId());
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //更新图片
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(pageNum, pageSize),
                pictureService.getQueryWrapper(queryRequest));
        Page<PictureVO> pictureVOPage = new Page<>(pageNum, pageSize, picturePage.getTotal());
        pictureVOPage.setRecords(pictureService.getPictureVOList(picturePage.getRecords()));
        return ResultUtils.success(pictureVOPage);
    }

}
