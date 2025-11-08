package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaowei.weipicturebackend.config.UnsplashConfig;
import com.xiaowei.weipicturebackend.enums.PictureReviewStatusEnum;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.upload.FileUploadPicture;
import com.xiaowei.weipicturebackend.manager.upload.PictureUploadTemplate;
import com.xiaowei.weipicturebackend.manager.upload.UrlUploadPicture;
import com.xiaowei.weipicturebackend.model.dto.file.UploadPictureResult;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureQueryRequest;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureReviewRequest;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureUploadRequest;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.mapper.PictureMapper;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.PictureVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 图片 服务实现类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-05
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FileUploadPicture fileUploadPicture;

    @Resource
    private UrlUploadPicture urlUploadPicture;

    @Resource
    private UnsplashConfig unsplashConfig;

    @Override
    @Transactional
    public Picture uploadPicture(Object inputSource,
                                 PictureUploadRequest uploadPictureRequest,
                                 HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        Long pictureId = null;
        pictureId = uploadPictureRequest.getId();
        //上传路径前缀
        String pathPrefix = String.format("%s/%s", "public", loginUser.getId());
        //根据inputResource区分上传方式
        PictureUploadTemplate pictureUploadTemplate=fileUploadPicture;
        if(inputSource instanceof String) {
            pictureUploadTemplate = urlUploadPicture;
        }
        //上传图片
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, pathPrefix);
        //构造图片信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUser.getId());
        //设置图片名称（抓取图片时使用）
        if(StrUtil.isNotBlank(uploadPictureRequest.getPicName())){
            picture.setName(uploadPictureRequest.getPicName());
        }
        //判断是新增图片还是更新
        if (pictureId != null) {
            //更新图片
            boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            //添加编辑时间和图片id
            picture.setEditTime(LocalDateTime.now());
            picture.setId(pictureId);
        }
        //填充审核信息，无论添加图片还是更新图片
        this.fillReviewParam(picture, loginUser);
        //插入数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return picture;
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }

    @Override
    public List<PictureVO> getPictureVOList(List<Picture> pictureList) {
        List<PictureVO> collect = pictureList.stream().map(this::getPictureVO).toList();
        //将创建图片的用户信息封装到pictureVO
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        if(CollectionUtil.isEmpty(userIdSet)){
            return collect;
        }
        Map<Long, List<User>> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        return collect.stream()
                .map(pictureVO -> {
                    if (userMap.containsKey(pictureVO.getUserId())) {
                        User user = userMap.get(pictureVO.getUserId()).get(0);
                        UserVO userVO = userService.getUserVO(user);
                        pictureVO.setUser(userVO);
                    }
                    return pictureVO;
                }).toList();
    }

    @Override
    public Wrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>()
                .eq(id != null, Picture::getId, id)
                .eq(reviewStatus!=null,Picture::getReviewStatus,reviewStatus)
                .like(StrUtil.isNotBlank(name), Picture::getName, name)
                .like(StrUtil.isNotBlank(introduction), Picture::getIntroduction, introduction)
                .eq(StrUtil.isNotBlank(category), Picture::getCategory, category)
                .eq(!(picSize ==null), Picture::getPicSize, picSize)
                .eq(!(picWidth ==null), Picture::getPicWidth, picWidth)
                .eq(!(picHeight ==null), Picture::getPicHeight, picHeight)
                .eq(!(picScale ==null), Picture::getPicScale, picScale)
                .like(StrUtil.isNotBlank(picFormat), Picture::getPicFormat, picFormat)
                .eq(!(userId==null), Picture::getUserId, userId)
                .like(StrUtil.isNotBlank(reviewMessage), Picture::getReviewMessage, reviewMessage)
                .eq(!(reviewerId==null), Picture::getReviewerId, reviewerId);
        //JSON数组查询
        if (CollectionUtil.isNotEmpty(tags)) {
            tags.forEach(tag -> {
                queryWrapper.like(StrUtil.isNotBlank(tag),Picture::getTags, "\""+tag+"\"");
            });
        }
        //从多字段中搜索
        if(!StrUtil.isBlank(searchText)){
            queryWrapper.and(wrapper -> wrapper.like(Picture::getName, searchText).or().like(Picture::getIntroduction, searchText));
        }

        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"),Picture::getCreateTime);


        return queryWrapper;
    }

    @Override
    public void doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.参数校验
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getPictureReviewStatusEnum(reviewStatus);
        if (pictureReviewStatusEnum == null || id == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //3.判断图片审核状态，不能重复审核
        if (oldPicture.getReviewStatus().equals(pictureReviewStatusEnum.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能重复审核");
        }
        //4.更新图片审核信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        //添加审核时间和审核人的id
        picture.setReviewTime(LocalDateTime.now());
        picture.setReviewerId(loginUser.getId());
        boolean updateResult = this.updateById(picture);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParam(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动审核通过");
            picture.setReviewTime(LocalDateTime.now());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                        HttpServletRequest request) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText),ErrorCode.PARAMS_ERROR,"搜索内容不能为空");

        //1.调用Unsplash API获取图片url
        String url = String.format(
                "https://api.unsplash.com/search/photos?query=%s&per_page=%s&client_id=%s",
                searchText,
                count,
                unsplashConfig.getAccessKey()
        );

        // 发送HTTP GET请求获取图片数据
        String result = HttpUtil.get(url);
        // 解析JSON数据
        JSONObject json = JSONUtil.parseObj(result);
        JSONArray results = json.getJSONArray("results");

        int uploadCount=0;
        int failCount=0;
        //2.上传图片
        for (int i = 0; i < results.size(); i++) {
            try {
                JSONObject photo = results.getJSONObject(i);
                // 获取图片URL（regular尺寸，适合展示）
                String imageUrl = photo.getJSONObject("urls").getStr("regular");

                //生成图片名称
                if(StrUtil.isBlank(namePrefix)){
                    namePrefix= searchText;
                }
                String picName= namePrefix+(i+1);
                //生成上传请求
                PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                pictureUploadRequest.setFileUrl(imageUrl);
                pictureUploadRequest.setPicName(picName);
                //调用上传服务上传图片
                this.uploadPicture(imageUrl, pictureUploadRequest, request);
                uploadCount++;
            } catch (Exception e) {
                log.error("上传第{}图片失败：{}",i+1, e.getMessage(),e);
                failCount++;
            }
        }

        //无论上传成功还是失败，都要返回结果
        log.info("批量上传图片，成功{}张，失败{}张",uploadCount,failCount);
        return uploadCount;
    }
}
