package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.enums.SpaceUserRoleEnums;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xiaowei.weipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.mapper.SpaceUserMapper;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.SpaceUserVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.model.vo.space.SpaceVO;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 空间用户关联 服务实现类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-27
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        if (ObjUtil.isEmpty(spaceUser)) {
            return null;
        }
        SpaceUserVO spaceUserVO=new SpaceUserVO();
        BeanUtil.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if(CollUtil.isEmpty(spaceUserList)){
            return List.of();
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(this::getSpaceUserVO).toList();
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());
        //如果userId或者spaceId的set集合为空，说明数据不存在，返回空集合
        if (CollUtil.isEmpty(userIdSet) || CollUtil.isEmpty(spaceIdSet)) {
            return List.of();
        }
        //分别一次查询获取user和space
        Map<Long, List<User>> userListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        //填充spaceUserVO的用户信息和空间信息

        spaceUserVOList.forEach(spaceUserVO -> {
            Long spaceId=spaceUserVO.getSpaceId();
            Long userId=spaceUserVO.getUserId();

            if(userListMap.containsKey(userId)){
                UserVO userVO = userService.getUserVO(userListMap.get(userId).get(0));
                spaceUserVO.setUser(userVO);
            }
            if(spaceListMap.containsKey(spaceId)){
                SpaceVO spaceVO = spaceService.getSpaceVO(spaceListMap.get(spaceId).get(0));
                spaceUserVO.setSpace(spaceVO);
            }
        });
        return spaceUserVOList;
    }

    @Override
    public Wrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest queryRequest) {
        Long id = queryRequest.getId();
        Long spaceId = queryRequest.getSpaceId();
        Long userId = queryRequest.getUserId();
        String spaceRole = queryRequest.getSpaceRole();
        return new LambdaQueryWrapper<SpaceUser>()
                .eq(ObjUtil.isNotNull(id), SpaceUser::getId, id)
                .eq(ObjUtil.isNotNull(spaceId), SpaceUser::getSpaceId, spaceId)
                .eq(ObjUtil.isNotNull(userId), SpaceUser::getUserId, userId)
                .eq(ObjUtil.isNotNull(spaceRole), SpaceUser::getSpaceRole, spaceRole);
    }

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        //校验参数
        Long spaceId = spaceUserAddRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId==null || spaceId<=0,ErrorCode.PARAMS_ERROR);
        Long userId = spaceUserAddRequest.getUserId();
        ThrowUtils.throwIf(userId==null || userId<=0,ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        //校验空间成员
        this.validSpaceUser(spaceUser,true);
        //添加到数据库
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser==null,ErrorCode.PARAMS_ERROR);
        //创建空间成员时，spaceId和userId必填
        Long spaceId=spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if(add){
            ThrowUtils.throwIf(spaceId==null || spaceId<=0,ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(userId==null || userId<=0,ErrorCode.PARAMS_ERROR);
            //检查关联的用户和空间是否存在
            if(!userService.lambdaQuery().eq(User::getId, userId).exists()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"关联的用户不存在");
            }
            if(!spaceService.lambdaQuery().eq(Space::getId, spaceId).exists()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"关联的空间不存在");
            }
        }
        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceUserRoleEnums spaceUserRoleEnumByValue = SpaceUserRoleEnums.getSpaceUserRoleEnumByValue(spaceRole);
        if (spaceUserRoleEnumByValue==null && spaceRole!=null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不存在该类型的空间角色");
        }
    }

}
