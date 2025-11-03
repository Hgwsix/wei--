package com.xiaowei.weipicturebackend.service.impl;

import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.mapper.UserMapper;
import com.xiaowei.weipicturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

}
