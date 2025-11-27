package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.mapper.SpaceMapper;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.*;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.*;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceAnalyzeService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //分析整个图库或者公共图库
        if(spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()){
            //只有管理员可以用
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR,"没有权限分析");
            //构造查询条件
            LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.select(Picture::getPicSize);
            if(!spaceUsageAnalyzeRequest.isQueryAll()){
                lambdaQueryWrapper.isNull(Picture::getSpaceId);
            }
            //查询
            List<Object> objectList = pictureService.getBaseMapper().selectObjs(lambdaQueryWrapper);
            long usedSize = objectList.stream()
                    .mapToLong(size -> size instanceof Long ? (Long) size : 0L)
                    .sum();
            long usedCount = objectList.size();
            //构造响应类
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //分析整个图库和公共图库时没有容量和数量的上限
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            //返回结果
            return spaceUsageAnalyzeResponse;
        }else{
            //分析个人空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            //参数校验
            ThrowUtils.throwIf(spaceId==null || spaceId<=0, ErrorCode.PARAMS_ERROR);
            //权限校验，只有用户自己和管理员可以分析
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(space,loginUser);
            //构造返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            //返回结果
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        //检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper
                .select("category AS category,COUNT(*) AS count ,SUM(picSize) AS totalSize")
                .groupBy("category");
        //查询并封装到响应类
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result->{
                    String category= result.get("category")==null?"未分类":result.get("category").toString();
                    Long count = result.get("count")==null?0L:Long.parseLong(result.get("count").toString());
                    Long totalSize = result.get("totalSize")==null?0L:Long.parseLong(result.get("totalSize").toString());
                    return new SpaceCategoryAnalyzeResponse(category,count,totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //根据范围补充查询条件
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        //查询
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .toList();
        //合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag->tag, Collectors.counting()));
        //返回响应类并降序排序
        return tagCountMap.entrySet().stream()
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        //根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        //查询
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(result -> Long.valueOf(result.toString()))
                .toList();
        //将对应范围的图片数量封装到响应类并返回
        LinkedHashMap<String,Long> sizeRangeMap = new LinkedHashMap<>();
        sizeRangeMap.put("<100KB",picSizeList.stream().filter(size->size<100*1024L).count());
        sizeRangeMap.put("100-500KB",picSizeList.stream().filter(size->size>=100*1024L&&size<500*1024L).count());
        sizeRangeMap.put("500KB-1MB",picSizeList.stream().filter(size->size>=500*1024L&&size<1024*1024L).count());
        sizeRangeMap.put(">1MB",picSizeList.stream().filter(size->size>=1024*1024L).count());
        return sizeRangeMap.entrySet()
                .stream()
                .map(entry->new SpaceSizeAnalyzeResponse(entry.getKey(),entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        //权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //判断用户是否存在
        Long userId = spaceUserAnalyzeRequest.getUserId();
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "空间图片用户行为分析的id字段为空");
        Optional.ofNullable(userService.getById(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId", userId);
        //分析维度：每日，每周，每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch(timeDimension){
            case "day": {
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period", "count(*) as count");
                break;
            }
            case "week":{
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            }
            case "month":{
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period", "count(*) as count");
                break;
            }
            default: throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持该时间维度的分析");
        }
        //根据分析范围填充查询条件
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        //分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");
        //查询并返回响应类
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result->{
                    String period = Optional.ofNullable(result.get("period").toString())
                            .map(Object::toString)
                            .orElse("");
                    Long count =Optional.ofNullable(result.get("count"))
                            .map(obj->Long.valueOf(obj.toString()))
                            .orElse(0L);
                    return new SpaceUserAnalyzeResponse(period,count);
                })
                .toList();
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        //参数校验
        Integer topN = spaceRankAnalyzeRequest.getTopN();
        ThrowUtils.throwIf(topN==null ||topN<=0,ErrorCode.PARAMS_ERROR, "topN参数错误");
        //权限校验，只有管理员可以使用
        if(!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限使用该功能");
        }
        //构造查询条件
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<Space>()
                .select(Space::getId,Space::getSpaceName,Space::getUserId,Space::getTotalSize)
                .orderByDesc(Space::getTotalSize)
                .last("limit "+topN);
        //查询并返回结果
        List<Space> spaceList = spaceService.list(queryWrapper);
        if (CollUtil.isEmpty(spaceList)) {
            return List.of();
        }
        return spaceList;
    }

    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser){
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //管理员专用，分析公共图库和全体空间
        if(queryAll || queryPublic ){
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }
        else{
            //私有空间权限校验
            //只有用户和管理员可以分析空间
            ThrowUtils.throwIf(spaceId==null||spaceId<=0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(space,loginUser);
        }
    }

    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                                QueryWrapper<Picture> queryWrapper){
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if(queryAll){
            //管理员分析全部空间
            return;
        }
        if(queryPublic){
            //管理员分析公共图库
            queryWrapper.isNull("spaceId");
            return;
        }
        if(spaceId!=null){
            //用户分析自己的空间，管理员也可分析
            queryWrapper.eq("spaceId",spaceId);
        }
    }
}

