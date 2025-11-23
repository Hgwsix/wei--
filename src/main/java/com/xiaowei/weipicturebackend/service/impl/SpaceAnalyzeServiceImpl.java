package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.mapper.SpaceMapper;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xiaowei.weipicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceAnalyzeService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

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

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
        //参数校验
        ThrowUtils.throwIf(spaceId==null || spaceId<=0, ErrorCode.PARAMS_ERROR);
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
            spaceUsageAnalyzeResponse.setSizeUsageRadio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRadio(null);
            //返回结果
            return spaceUsageAnalyzeResponse;
        }else{
            //分析个人空间
            //权限校验，只有用户自己和管理员可以分析
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(space,loginUser);
            //构造返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getTotalCount());
            //计算比例
            double sizeUsageRadio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRadio(sizeUsageRadio);
            double countUsageRadio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRadio(countUsageRadio);
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
                .map(result -> {
                    return result == null ? "未打上标签" : result.toString();
                })
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
}

