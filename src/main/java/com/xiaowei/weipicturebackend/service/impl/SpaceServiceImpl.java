package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowei.weipicturebackend.enums.SpaceLevelEnums;
import com.xiaowei.weipicturebackend.enums.SpaceTypeEnums;
import com.xiaowei.weipicturebackend.enums.SpaceUserRoleEnums;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceAddRequest;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.mapper.SpaceMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.space.SpaceVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>
 * 空间 服务实现类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-15
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    private final Map<Long,Object> lockMap=new ConcurrentHashMap<>();

    @Override
    public SpaceVO getSpaceVO(Space Space) {
        if(Space == null){
            return null;
        }
        SpaceVO spaceVO=new SpaceVO();
        BeanUtil.copyProperties(Space,spaceVO);
        return spaceVO;
    }

    @Override
    public List<SpaceVO> getSpaceVOList(List<Space> spaceList) {
        //将space转成spaceVO
        List<SpaceVO> spaceVOList = spaceList.stream().map(this::getSpaceVO).toList();
        //从数据库中查对应的空间创建人信息
        Set<Long> userIds = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
        if(CollUtil.isEmpty(userIds)){
            return List.of();
        }
        Map<Long, List<User>> collect = userService.listByIds(userIds).stream().collect(Collectors.groupingBy(User::getId));
        //将创建人信息封装到VO类中
        spaceVOList.forEach(spaceVO -> {
            if(collect.containsKey(spaceVO.getUserId())){
                User user = collect.get(spaceVO.getUserId()).get(0);
                UserVO userVO = userService.getUserVO(user);
                spaceVO.setUser(userVO);
            }
        });
        return spaceVOList;
    }

    @Override
    public Wrapper<Space> getQueryWrapper(SpaceQueryRequest queryRequest) {
        Long id = queryRequest.getId();
        String spaceName = queryRequest.getSpaceName();
        Integer spaceLevel = queryRequest.getSpaceLevel();
        Long userId = queryRequest.getUserId();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        Integer spaceType = queryRequest.getSpaceType();

        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<Space>()
                .eq(id != null, Space::getId, id)
                .like(StrUtil.isNotBlank(spaceName), Space::getSpaceName, spaceName)
                .eq(spaceLevel != null, Space::getSpaceLevel, spaceLevel)
                .eq(userId != null, Space::getUserId, userId)
                .eq(spaceType != null, Space::getSpaceType, spaceType)
                .orderBy(StrUtil.isNotBlank(sortField), StrUtil.equals(sortOrder, "ascend"), Space::getCreateTime);



        return queryWrapper;
    }

    @Override
    public Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest queryRequest) {

        int current = queryRequest.getCurrent();
        int pageSize = queryRequest.getPageSize();
        Page<Space> spacePage = this.page(Page.of(current, pageSize), this.getQueryWrapper(queryRequest));

        Page<SpaceVO> spaceVOPage = Page.of(current, pageSize, spacePage.getTotal());
        spaceVOPage.setRecords(this.getSpaceVOList(spacePage.getRecords()));

        return spaceVOPage;
    }

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request, Long loginUserId) {
        //为空间添加默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnums.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            //不传入空间类型时默认创建私人空间
            spaceAddRequest.setSpaceType(SpaceTypeEnums.PRIVATE.getValue());
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest,space);
        //1.自动填充空间限额
        this.fillSpaceLevel(space);
        //2.校验空间数据
        this.validSpace(space, true);
        //3.获取当前登入用户
        User loginUser=null;
        if(request==null && loginUserId!=null){
            loginUser = userService.getById(loginUserId);
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
            }
        }else {
            loginUser = userService.getLoginUser(request);
        }
        //4.权限校验，普通用户只能创建普通级别的空间
        if(!userService.isAdmin(loginUser) && !SpaceLevelEnums.COMMON.getValue().equals(space.getSpaceLevel())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建该级别空间");
        }

        Long userId=loginUser.getId();
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                transactionTemplate.execute(status -> {
                    //5.判断用户是否已经存在空间（每人只能创建一个空间）
                    boolean exists = lambdaQuery()
                            .eq(Space::getUserId, loginUserId)
                            .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                            .exists();
                    if (exists) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                    }
                    //6.保存到数据库
                    space.setUserId(userId);
                    boolean saveResult = this.save(space);
                    ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "添加空间失败");
                    //如果创建的是团队空间，创建者为管理员，自动在关联表新增一条信息
                    if(SpaceTypeEnums.TEAM.getValue().equals(space.getSpaceType())){
                        SpaceUser spaceUser=new SpaceUser();
                        spaceUser.setUserId(loginUserId);
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setSpaceRole(SpaceUserRoleEnums.ADMIN.getValue());
                        //操作数据库
                        boolean save = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
                    }
                    return null;
                });
            } finally {
                lockMap.remove(userId);
            }
        }
        return space.getId();

    }

    @Override
    public void fillSpaceLevel(Space space) {
        //根据空间级别，自动填充限额
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnums spaceLevelEnumByValue = SpaceLevelEnums.getSpaceLevelEnumByValue(spaceLevel);
        //如果管理员自己设置空间限额，那么根据管理员填入的数据填充
        if (spaceLevelEnumByValue!=null) {
            Long maxCount = space.getMaxCount();
            Long maxSize = space.getMaxSize();
            if(maxCount==null){
                space.setMaxCount(spaceLevelEnumByValue.getMaxCount());
            }
            if(maxSize==null){
                space.setMaxSize(spaceLevelEnumByValue.getMaxSize());
            }
        }

    }

    @Override
    public void validSpace(Space space, boolean add) {
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnums spaceLevelEnumByValue = SpaceLevelEnums.getSpaceLevelEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnums spaceTypeEnumByValue = SpaceTypeEnums.getSpaceTypeEnumByValue(spaceType);
        //如果是创建空间
        if (add) {
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "请选择空间级别");
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceName.length()>30, ErrorCode.PARAMS_ERROR, "空间名称过长");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "请选择空间类型");
        }
        //如果是更新空间
        else{
            ThrowUtils.throwIf(spaceLevel!=null&&spaceLevelEnumByValue == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
            ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName)&&spaceName.length()>30, ErrorCode.PARAMS_ERROR, "空间名称过长");
            if (spaceType != null && spaceTypeEnumByValue == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");

            }
        }


    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        //只有管理员和本人可以操作
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限编辑该空间");
        }
    }


}
