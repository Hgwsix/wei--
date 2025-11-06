package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.FileManager;
import com.xiaowei.weipicturebackend.model.dto.file.UploadPictureResult;
import com.xiaowei.weipicturebackend.model.dto.picture.PictureQueryRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 图片 服务实现类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-05
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FileManager fileManager;

    @Override
    @Transactional
    public Picture uploadPicture(MultipartFile multipartFile,
                                 PictureUploadRequest uploadPictureRequest,
                                 HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        Long pictureId = null;
        pictureId = uploadPictureRequest.getId();
        //上传路径前缀
        String pathPrefix = String.format("%s/%s", "public", loginUser.getId());
        //上传图片
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, pathPrefix);
        //构造图片信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUser.getId());
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
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
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
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>()
                .eq(id != null, Picture::getId, id)
                .like(StrUtil.isNotBlank(name), Picture::getName, name)
                .like(StrUtil.isNotBlank(introduction), Picture::getIntroduction, introduction)
                .eq(StrUtil.isNotBlank(category), Picture::getCategory, category)
                .eq(!(picSize ==null), Picture::getPicSize, picSize)
                .eq(!(picWidth ==null), Picture::getPicWidth, picWidth)
                .eq(!(picHeight ==null), Picture::getPicHeight, picHeight)
                .eq(!(picScale ==null), Picture::getPicScale, picScale)
                .like(StrUtil.isNotBlank(picFormat), Picture::getPicFormat, picFormat)
                .eq(!(userId==null), Picture::getUserId, userId);
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


        return null;
    }
}
