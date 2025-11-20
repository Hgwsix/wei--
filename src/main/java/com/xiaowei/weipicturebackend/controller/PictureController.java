package com.xiaowei.weipicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.annotation.AuthCheck;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.DeleteRequest;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.enums.PictureReviewStatusEnum;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.picture.*;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.PictureVO;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    @Resource
    private SpaceService spaceService;

    /**
     * 上传图片（可以重复上传）
     *
     * @param multipartFile
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 @ModelAttribute PictureUploadRequest uploadPictureRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.uploadPicture(multipartFile, uploadPictureRequest, request);
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }
    /**
     * url上传图片（可以重复上传）
     *
     * @param
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest uploadPictureRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(uploadPictureRequest == null, ErrorCode.PARAMS_ERROR);
        String fileUrl= uploadPictureRequest.getFileUrl();
        Picture picture = pictureService.uploadPicture(fileUrl, uploadPictureRequest, request);
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
        //检查是否存在数据
        Picture oldPicture = pictureService.getById(id);
        Long spaceId = oldPicture.getSpaceId();
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据不存在");
        }
        //检查权限
        pictureService.checkPictureAuth(oldPicture, loginUser);
        //删除图片
        boolean remove = pictureService.removeById(id);
        if (!remove) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        //删除成功，更新空间额度
        if(spaceId != null){
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, spaceId)
                    .setSql("totalCount=totalCount - 1")
                    .setSql("totalSize=totalSize - " + oldPicture.getPicSize())
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR,"额度更新失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest updateRequest,
                                               HttpServletRequest request) {
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
        //填充审核信息
        pictureService.fillReviewParam(picture, userService.getLoginUser(request));
        //更新图片
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 分页获取图片信息（封装类）
     * @param queryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest queryRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(pictureService.listPictureVOByPage(queryRequest,request));
    }

    /**
     * 分页获取图片信息（管理员可用）
     * @param queryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = queryRequest.getCurrent();
        int pageSize = queryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(queryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 根据id获取图片封装类
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestParam Long id,HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //检查权限
        if(picture.getSpaceId()!=null){
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(picture, loginUser);
        }
        //只能查看已经审核通过的图片
        ThrowUtils.throwIf(!picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue()), ErrorCode.NO_AUTH_ERROR);
        //将用户脱敏后信息封装进图片封装类
        PictureVO pictureVO = pictureService.getPictureVO(picture);
        Long userId = picture.getUserId();
        User byId = userService.getById(userId);
        if(byId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        pictureVO.setUser(userService.getUserVO(byId));
        return ResultUtils.success(pictureVO);

    }
    /**
     * 根据id获取图片信息（管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null, ErrorCode.PARAMS_ERROR);
        //1.获取编辑信息
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(id==null || id<=0,ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        BeanUtil.copyProperties(pictureEditRequest, oldPicture);
        //将tags数组转JSON字符串
        oldPicture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //对编辑的信息进行数据校验
        validPicture(oldPicture);
        //2.校验信息，只有本人和管理员可以编辑
        User loginUser = userService.getLoginUser(request);

        pictureService.checkPictureAuth(oldPicture, loginUser);
        //校验空间权限，只有空间管理人可以编辑图片
        Long spaceId = pictureEditRequest.getSpaceId();
        if(spaceId!=null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "没有权限编辑该图片");
        }
        //4.设置编辑时间
        oldPicture.setEditTime(LocalDateTime.now());
        //5.更新数据库
        //填充审核信息
        pictureService.fillReviewParam(oldPicture, loginUser);
        boolean update = pictureService.updateById(oldPicture);
        ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"编辑失败");
        return ResultUtils.success(true);
    }

    /**
     * 验证Picture对象的有效性
     *
     * @param picture 需要验证的Picture对象
     */
    public void validPicture(Picture picture) {
        // 检查picture对象是否为null
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        // 获取picture对象的id和introduction属性
        Long id = picture.getId();
        String introduction = picture.getIntroduction();
        // 检查id是否为0或null
        ThrowUtils.throwIf(id == 0 || id == null, ErrorCode.PARAMS_ERROR);
        // 检查introduction不为空且长度超过800的情况
        ThrowUtils.throwIf(StrUtil.isNotBlank(introduction) && introduction.length() > 800, ErrorCode.PARAMS_ERROR);
    }
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片（仅管理员）
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doReviewPicture(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片（仅管理员）
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureService.uploadPictureByBatch(pictureUploadByBatchRequest,request));
    }

    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody PictureSearchPictureByColorRequest pictureSearchPictureByColorRequest,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureSearchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureService.searchPictureByColor(pictureSearchPictureByColorRequest, request));
    }

    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

}
