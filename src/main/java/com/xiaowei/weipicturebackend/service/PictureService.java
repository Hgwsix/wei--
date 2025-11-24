package com.xiaowei.weipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunCreateOutPaintingTaskRequest;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunCreateTaskResponse;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunQueryTaskResponse;
import com.xiaowei.weipicturebackend.model.dto.picture.*;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * <p>
 * 图片 服务类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-05
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片的方法
     *
     * @param
     * @param uploadPictureRequest 上传图片的请求参数封装对象
     * @param request              HttpServletRequest对象，用于获取请求相关信息
     * @return PictureVO 返回上传后的图片信息封装对象
     */
    Picture uploadPicture(Object inputSource, PictureUploadRequest uploadPictureRequest, HttpServletRequest request);

    /**
     * 根据Picture对象获取对应的PictureVO对象
     * PictureVO可能是用于视图展示的对象，包含了Picture的部分或全部属性
     *
     * @param picture 包含图片信息的实体对象
     * @return 返回一个用于视图展示的PictureVO对象
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 根据图片列表获取对应的视图对象列表
     *
     * @param pictureList 图片实体对象列表
     * @return 返回图片视图对象列表，用于前端展示
     */
    List<PictureVO> getPictureVOList(List<Picture> pictureList);

    /**
     * 根据查询请求参数构建Picture实体的查询条件封装器
     *
     * @param
     * @return Wrapper<Picture> 返回一个Picture实体的查询条件封装器，用于数据库查询操作
     */
    Wrapper<Picture> getQueryWrapper(PictureQueryRequest queryRequest);

    /**
     * 执行图片审核功能
     *
     * @param pictureReviewRequest 图片审核请求对象，包含需要审核的图片相关信息
     * @param loginUser            当前登录用户对象，用于获取用户信息和权限验证
     */
    void doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数方法
     *
     * @param picture   图片对象，包含需要评论的图片信息
     * @param loginUser 登录用户对象，包含当前登录用户的信息
     */
    void fillReviewParam(Picture picture, User loginUser);

    /**
     * 批量上传图片方法
     * <p>
     * 该方法用于处理批量图片上传请求，接收一个包含上传参数的PictureUploadRequest对象，
     * 并返回一个Integer类型的上传结果。
     *
     * @param pictureUploadByBatchRequest 包含图片上传所需参数的请求对象，如图片数据、上传配置等
     * @return 返回一个Integer类型的结果，可能表示上传成功的数量、状态码或其他业务相关数值
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request);

    /**
     * 分页查询图片视图对象列表
     *
     * @param queryRequest 图片查询请求参数，包含分页、排序和查询条件等信息
     * @return 返回一个包含PictureVO对象的分页结果，包含数据列表和分页信息
     */
    Page<PictureVO> listPictureVOByPage(PictureQueryRequest queryRequest, HttpServletRequest request);

    /**
     * 检查用户对图片的访问权限
     *
     * @param picture   需要检查权限的图片对象
     * @param loginUser 当前登录用户对象
     *                  该方法用于验证用户是否有权限访问指定的图片，可能会抛出权限相关的异常
     */
    void checkPictureAuth(Picture picture, User loginUser);

    /**
     * 清除图片文件的方法
     *
     * @param picture 需要清除的图片对象
     */
    void clearPictureFile(Picture picture);

    /**
     * 根据颜色搜索图片的方法
     *
     * @param pictureSearchPictureByColorRequest 包含颜色搜索条件的请求对象
     * @return 返回符合条件的图片列表
     */
    List<PictureVO> searchPictureByColor(PictureSearchPictureByColorRequest pictureSearchPictureByColorRequest, HttpServletRequest request);

    /**
     * 批量编辑图片的方法
     *
     * @param pictureEditByBatchRequest 图片编辑请求参数对象，包含需要批量编辑的图片信息和编辑规则
     * @param loginUser                 当前登录用户对象，用于验证用户权限和记录操作日志
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建图片外绘任务
     *
     * @param createPictureOutPaintingRequest 阿里云创建外绘任务请求参数
     * @param loginUser                       登录用户信息
     * @return AliyunCreateTaskResponse 阿里云创建任务响应结果
     */
    AliyunCreateTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingRequest createPictureOutPaintingRequest, User loginUser);
}


