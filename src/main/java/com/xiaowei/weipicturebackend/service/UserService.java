package com.xiaowei.weipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaowei.weipicturebackend.model.dto.user.UserQueryRequest;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowei.weipicturebackend.model.vo.LoginUserVO;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author xiaowei
 * @since 2025-11-03
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册方法
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册结果，返回long类型数据，可能是用户ID或状态码
     */
    long userRegister(String userAccount, String userPassword, String checkPassword,HttpServletRequest request);

    /**
     * 获取加密后的密码字符串
     * 该方法接收用户输入的原始密码，并返回经过加密处理后的密码字符串
     *
     * @param userPassword 用户输入的原始密码字符串
     * @return 返回经过加密处理后的密码字符串
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录方法
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象，用于获取请求相关信息
     * @return 返回登录后的User对象
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 根据用户信息获取登录用户视图对象
     * 该方法用于将用户实体转换为前端所需的登录用户视图对象
     *
     * @param user 用户实体对象，包含用户的基本信息
     * @return LoginUserVO 登录用户视图对象，用于前端展示的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 根据HTTP请求获取登录用户信息
     *
     * @param request HTTP请求对象，包含请求相关的所有信息
     * @return LoginUserVO 登录用户的信息视图对象，包含用户的基本信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 注销用户登录状态的方法
     *
     * @param request HttpServletRequest对象，包含客户端请求信息
     * @return Boolean 返回操作结果，true表示注销成功，false表示注销失败
     */
    Boolean logoutUser(HttpServletRequest request);

    /**
     * 根据User对象获取对应的UserVO对象
     * UserVO通常用于前端展示，是对User对象的数据封装和转换
     *
     * @param user 用户实体对象，包含完整的用户信息
     * @return UserVO 视图对象，包含需要展示给用户的信息
     */
    UserVO getUserVO(User user);

    /**
     * 根据用户列表获取用户视图对象列表
     *
     * @param userList 用户实体列表，包含完整的用户信息
     * @return 用户视图对象列表，通常包含前端需要的部分用户信息
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据用户查询请求条件构造Lambda查询包装器
     *
     * @param userQueryRequest 用户查询请求对象，包含查询条件
     * @return 返回一个LambdaQueryWrapper<User>对象，用于构建数据库查询条件
     */
    LambdaQueryWrapper<User> getUserQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断当前请求用户是否为管理员
     *
     * @param
     * @return 如果用户是管理员则返回true，否则返回false
     */
    boolean isAdmin(User user);
}
