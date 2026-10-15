package com.xiaowei.weipicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.enums.SpaceLevelEnums;
import com.xiaowei.weipicturebackend.enums.SpaceTypeEnums;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.auth.StpKit;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceAddRequest;
import com.xiaowei.weipicturebackend.model.dto.space.SpaceEditRequest;
import com.xiaowei.weipicturebackend.model.dto.user.UserInfoEditRequest;
import com.xiaowei.weipicturebackend.model.dto.user.UserQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.mapper.UserMapper;
import com.xiaowei.weipicturebackend.model.vo.LoginUserVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-03
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Lazy
    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, HttpServletRequest request) {
        //1.参数校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        //2.查看账号是否重复
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, userAccount);
        long count = this.list(userLambdaQueryWrapper).size();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //3.加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        User user = new User();
        transactionTemplate.execute(status -> {
            //4.插入数据
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName("无名");
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败");
            }
            //用户注册成功之后，自动为其创建私人空间
            SpaceAddRequest spaceAddRequest = new SpaceAddRequest();
            spaceAddRequest.setSpaceName("默认空间");
            spaceAddRequest.setSpaceLevel(SpaceLevelEnums.COMMON.getValue());
            spaceAddRequest.setSpaceType(SpaceTypeEnums.PRIVATE.getValue());
            spaceService.addSpace(spaceAddRequest, null, user.getId());

            return null;
        });
        return user.getId();
    }


    /**
     * 重写getEncryptPassword方法，用于获取加密后的密码
     *
     * @param userPassword 用户输入的原始密码
     * @return 返回经过MD5加密后的密码字符串
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        //使用盐值加密
        String SALT = "xiaowei";
        return DigestUtil.md5Hex(SALT + userPassword);
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.参数校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        //2.加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        //3.检查用户是否存在
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, encryptPassword);
        User user = this.getOne(userLambdaQueryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        //4.记录用户登入状态
        request.getSession().setAttribute(UserConstants.USER_LOGIN_STATE, user);
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstants.USER_LOGIN_STATE, user);
        return user;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断用户是否已经登入
        Object object = request.getSession().getAttribute(UserConstants.USER_LOGIN_STATE);
        User user = (User) object;
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登入");
        }
        //获取登入信息并返回
        return user;
    }

    @Override
    public Boolean logoutUser(HttpServletRequest request) {
        //判断用户是否已经登入
        Object object = request.getSession().getAttribute(UserConstants.USER_LOGIN_STATE);
        User user = (User) object;
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登入");
        }
        //在session中除去用户的登入状态
        request.getSession().removeAttribute(UserConstants.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return List.of();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public LambdaQueryWrapper<User> getUserQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<User>()
                .eq(id != null, User::getId, id)
                .like(StrUtil.isNotBlank(userAccount), User::getUserAccount, userAccount)
                .like(StrUtil.isNotBlank(userName), User::getUserName, userName)
                .like(StrUtil.isNotBlank(userProfile), User::getUserProfile, userProfile)
                .eq(StrUtil.isNotBlank(userRole), User::getUserRole, userRole)
                .orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), User::getCreateTime);
        return userLambdaQueryWrapper;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null) {
            return false;
        }
        return UserConstants.ADMIN_ROLE.equals(loginUser.getUserRole());
    }

    @Override
    public boolean userInfoEdit(HttpServletRequest request, UserInfoEditRequest userInfoEditRequest) {
        //1、判断用户是否登入
        User loginUser = this.getLoginUser(request);
        if(ObjUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登入才可编辑信息");
        }
        //2、只有用户本人和管理原可以编辑用户自己的信息
        Long editUserId = userInfoEditRequest.getUserId();
        User editUser = this.baseMapper.selectById(editUserId);
        if(ObjUtil.isNull(editUser)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编辑的用户信息不存在");
        }
        if(!UserConstants.ADMIN_ROLE.equals(loginUser.getUserRole()) && !loginUser.getId().equals(editUserId)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限编辑");
        }
        //3、构造用户信息
        BeanUtil.copyProperties(userInfoEditRequest, editUser);
        editUser.setEditTime(LocalDateTime.now());
        //4、入库
        boolean result = this.updateById(editUser);
        ThrowUtils.throwIf(!result , ErrorCode.SYSTEM_ERROR, "用户信息编辑失败");
        return result;
    }
}
