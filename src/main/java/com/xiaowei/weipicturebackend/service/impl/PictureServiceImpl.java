package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.config.UnsplashConfig;
import com.xiaowei.weipicturebackend.constant.PictureConstants;
import com.xiaowei.weipicturebackend.enums.PictureReviewStatusEnum;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.CosManager;
import com.xiaowei.weipicturebackend.manager.upload.FileUploadPicture;
import com.xiaowei.weipicturebackend.manager.upload.PictureUploadTemplate;
import com.xiaowei.weipicturebackend.manager.upload.UrlUploadPicture;
import com.xiaowei.weipicturebackend.model.dto.file.UploadPictureResult;
import com.xiaowei.weipicturebackend.model.dto.picture.*;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.mapper.PictureMapper;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.PictureVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import com.xiaowei.weipicturebackend.util.ColorSimilarityUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.PKIXCertPathBuilderResult;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            //缓存五分钟过期
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .maximumSize(10000L)
            .build();


    /**
     * 上传图片服务
     * spaceId为空时，上传到公共图库，否则上传到私人空间
     * @param inputSource
     * @param uploadPictureRequest 上传图片的请求参数封装对象
     * @param request              HttpServletRequest对象，用于获取请求相关信息
     * @return
     */
    @Override
    @Transactional
    public Picture uploadPicture(Object inputSource,
                                 PictureUploadRequest uploadPictureRequest,
                                 HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        Long pictureId = uploadPictureRequest.getId();
        //校验空间是否存在
        Long spaceId = uploadPictureRequest.getSpaceId();
        if (spaceId != null) {
            Space oldSpace=spaceService.getById(spaceId);
            if(oldSpace==null){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            }
            //校验空间是否属于当前用户
            if (!oldSpace.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限上传图片到该空间");
            }
            //校验空间容量
            if(oldSpace.getTotalCount()>=oldSpace.getMaxCount()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间容量已满");
            }
            if(oldSpace.getTotalSize()>=oldSpace.getMaxSize()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间容量已满");
            }
        }
        //上传路径前缀
        String pathPrefix ;
        if (spaceId != null) {
            pathPrefix=String.format("%s/%s","space",spaceId);
        }else pathPrefix = String.format("%s/%s", "public", loginUser.getId());
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
        picture.setSpaceId(spaceId);
        //设置图片名称（抓取图片时使用）
        if(StrUtil.isNotBlank(uploadPictureRequest.getPicName())){
            picture.setName(uploadPictureRequest.getPicName());
        }
        
        //判断是新增图片还是更新
        Picture oldPicture = null;
        if (pictureId != null) {
            //更新图片
            oldPicture = lambdaQuery().eq(Picture::getId, pictureId).getEntity();
            if (oldPicture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            //检查空间是否一致
            if (spaceId != null && !spaceId.equals(oldPicture.getSpaceId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片空间不一致");
            }
            //如果传入的spaceId为空，则复用之前的spaceId
            if (spaceId == null) {
                picture.setSpaceId(oldPicture.getSpaceId());
            }
            //添加编辑时间和图片id
            picture.setEditTime(LocalDateTime.now());
            picture.setId(pictureId);
            //更新图片后，将原本在云端的图片删除
            this.clearPictureFile(oldPicture);
        }
        //填充审核信息，无论添加图片还是更新图片
        this.fillReviewParam(picture, loginUser);
        //插入数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //如果是在空间中上传图片，则更新空间额度
        if(spaceId!=null) {
            Long spaceChangeSize = picture.getPicSize();
            if(oldPicture!=null){
                spaceChangeSize= picture.getPicSize()-oldPicture.getPicSize();
            }
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, spaceId)
                    .setSql("totalCount=totalCount + 1")
                    .setSql("totalSize=totalSize + " + spaceChangeSize)
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR,"额度更新失败");
        }
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

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
                .eq(!(reviewerId==null), Picture::getReviewerId, reviewerId)
                .eq(spaceId != null, Picture::getSpaceId, spaceId)
                .isNull(nullSpaceId, Picture::getSpaceId)
                //>=
                .ge(startEditTime != null, Picture::getEditTime, startEditTime)
                //<
                .lt(endEditTime != null, Picture::getEditTime, endEditTime);
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
            //普通用户编辑的图片都将被置为待审核
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

    @Override
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest queryRequest,HttpServletRequest request) {
        int current = queryRequest.getCurrent();
        int pageSize = queryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR);
        //构造缓存key
        String cacheKey = PictureConstants.PICTURE_CACHE_KEY + DigestUtil.md5Hex(JSONUtil.toJsonStr(queryRequest));
        //1.查询本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if(StrUtil.isNotBlank(cacheValue)){
            Page<PictureVO> pictureVO = JSONUtil.toBean(cacheValue, Page.class);
            return pictureVO;
        }
        //2.如果本地缓存不存在，查询Redis分布式缓存
        String redisValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if(StrUtil.isNotBlank(redisValue)){
            Page<PictureVO> pictureVO = JSONUtil.toBean(redisValue, Page.class);
            LOCAL_CACHE.put(cacheKey, JSONUtil.toJsonStr(pictureVO));
            return pictureVO;
        }
        String lockKey = String.format("lock:%s", cacheKey);
        //使用互斥锁防止缓存击穿
        RLock lock = redissonClient.getLock(lockKey);
        Page<PictureVO> pictureVOPage;
        try {

            try {
                //尝试获取锁(等待时间，持有时间，时间单位)
                boolean islocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!islocked) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            } catch (InterruptedException e) {
                log.error("获取锁失败：{}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            //3.如果redis缓存不存在，查询数据库
            Long spaceId = queryRequest.getSpaceId();
            //如果传入spaceId，则只能时空间管理员可以查询，且图片不需要过审
            if(spaceId!=null){
                //判断是否为空间管理员
                User loginUser = userService.getLoginUser(request);
                Space space = spaceService.getById(spaceId);
                if(space==null){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在");
                }
                ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()),ErrorCode.NO_AUTH_ERROR,"没有空间权限");
                queryRequest.setNullSpaceId(false);
            }
            //如果没有传入spaceId，则查询所有图片，需要过审
            else{
                //只能查询已经过审的照片
                queryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                queryRequest.setNullSpaceId(true);
            }
            Page<Picture> picturePage = this.page(new Page<>(current, pageSize),
                    this.getQueryWrapper(queryRequest));
            pictureVOPage = new Page<>(current, pageSize, picturePage.getTotal());
            pictureVOPage.setRecords(this.getPictureVOList(picturePage.getRecords()));
            //4.将查询结果存入本地缓存和redis缓存
            //更新caffeine缓存
            LOCAL_CACHE.put(cacheKey, JSONUtil.toJsonStr(pictureVOPage));
            //更新redis缓存(配置过期时间5到10分钟过期)
            int expireTime = 5 + RandomUtil.randomInt(0, 5);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(pictureVOPage), expireTime, TimeUnit.MINUTES);
        } finally {
            //释放锁
            lock.unlock();
        }
        return pictureVOPage;
    }

    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        if (spaceId==null) {
            //公共图片，仅本人和管理员可以操作图片
            if(!userService.isAdmin(loginUser) && !loginUser.getId().equals(picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            //私有图片，仅图片管理员
            if(!loginUser.getId().equals(picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void clearPictureFile(Picture picture) {
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        //获取图片在云端的url
        String url = picture.getUrl();
        //获取缩略图url
        String thumbnailUrl = picture.getThumbnailUrl();
        //调用删除对象服务删除云端文件
        cosManager.deleteObject(url);
        cosManager.deleteObject(thumbnailUrl);
    }

    @Override
    public List<PictureVO> searchPictureByColor(PictureSearchPictureByColorRequest pictureSearchPictureByColorRequest,
                                              HttpServletRequest request) {
        String picColor = pictureSearchPictureByColorRequest.getPicColor();
        Long spaceId = pictureSearchPictureByColorRequest.getSpaceId();
        //1.校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(picColor),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId==null || spaceId<=0,ErrorCode.PARAMS_ERROR);
        //2.校验空间权限
        User loginUser = userService.getLoginUser(request);
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null,ErrorCode.PARAMS_ERROR,"空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
        }
        //3.查询当前空间下所有图片，必须有主色调
        List<Picture> pictureList = lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        //如果没有查询到图片，返回空列表
        if(CollUtil.isEmpty(pictureList)){
            return List.of();
        }
        List<PictureVO> pictureVOList = getPictureVOList(pictureList);
        //4.计算空间相似度并排序
        return pictureVOList.stream()
                .sorted(Comparator.comparingDouble(pictureVO -> -ColorSimilarityUtil.calculateSimilarity(pictureVO.getPicColor(), picColor)))
                .limit(15)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        String category = pictureEditByBatchRequest.getCategory();
        String tags = JSONUtil.toJsonStr(pictureEditByBatchRequest.getTags());
        //1.校验参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId==null || spaceId<=0,ErrorCode.PARAMS_ERROR);
        //2.权限校验
        Space space = spaceService.getById(spaceId);
        if (space==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在");
        }
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
        }
        //3.查询需要修改的所有图片（只查询修改字段）
        List<Picture> pictureList = lambdaQuery()
                .select(Picture::getId,Picture::getSpaceId)
                .in(Picture::getId, pictureIdList)
                .eq(Picture::getSpaceId,spaceId)
                .list();
        if(CollUtil.isEmpty(pictureList)){
            return ;
        }
        //4.写入修改后的数据
        pictureList.forEach(picture -> {
            if(StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (StrUtil.isNotBlank(tags)) {
                picture.setTags(tags);
            }
        });
        //根据命名规则批量修改名字
        fillPictureWithNameRule(pictureList,nameRule);
        //5.操作数据库，批量更新数据
        boolean updateResult = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!updateResult,ErrorCode.SYSTEM_ERROR,"批量修改失败");
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                    String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

}

