package com.xiaowei.weipicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.enums.SpaceTypeEnums;
import com.xiaowei.weipicturebackend.enums.SpaceUserRoleEnums;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserAuthContext;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.SpaceUser;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //判断loginType,仅对Space进行权限校验
        if (!loginType.equals(StpKit.SPACE_TYPE)) {
            return new ArrayList<>();
        }
        //管理员权限，表示权限验证通过
        List<String> ADMIN_PERMISSIONS = SpaceUserAuthManager.getPermissionsByRole(SpaceUserRoleEnums.ADMIN.getValue());
        //获取上下文对象
        SpaceUserAuthContext contextByRequest = null;
        try {
            contextByRequest = getSpaceUserAuthContextByRequest();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //如果上下文对象为空，说明是公共图库，可以通过
        if (isAllFieldsNull(contextByRequest)) {
            return ADMIN_PERMISSIONS;
        }
        //优先从上下文中获取SpaceUser对象
        SpaceUser spaceUser = contextByRequest.getSpaceUser();
        if (spaceUser != null) {
            return SpaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        //获取当前登入用户的userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstants.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登入");
        }
        Long loginUserId = loginUser.getId();
        //SpaceUserId不为空，根据空间成员表获取响应权限
        if (contextByRequest.getSpaceUserId() != null) {
            SpaceUser spaceUserById = spaceUserService.getById(contextByRequest.getSpaceUserId());
            if (spaceUserById == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员信息不存在");
            }
            //查询当前登入用户在空间用户中的信息
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUserById.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            return SpaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        //如果SpaceUserId为空，尝试从SpaceId或者PictureId获取
        Long spaceId = contextByRequest.getSpaceId();
        if (spaceId == null) {
            //如果SpaceId为空，尝试从PictureId获取
            Long pictureId = contextByRequest.getPictureId();
            //如果pictureId也为空，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            //根据pictureId获取spaceId
            Picture pictureById = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getSpaceId, Picture::getUserId, Picture::getId)
                    .one();
            if (pictureById == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = pictureById.getSpaceId();
            //公共图库，仅本人和管理员可以操作
            if (spaceId == null) {
                if (pictureById.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }
                //不是自己的图片，仅可查看
                else {
                    return SpaceUserAuthManager.getPermissionsByRole(SpaceUserRoleEnums.VIEWER.getValue());
                }
            }
        }
        //获取space对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间信息不存在");
        }
        //判断空间类型
        Integer spaceType = space.getSpaceType();
        //私有空间，仅本人或管理员有权
        if (SpaceTypeEnums.PRIVATE.getValue().equals(spaceType)) {
            if (space.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
        }
        //团队空间，查询空间成员并返回对应的权限
        else {
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return SpaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        return ADMIN_PERMISSIONS;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }


    @Value("${spring.mvc.servlet.path}")
    private String contextPath;
    /**
     * 通过请求获取上下文信息
     * @return
     */
    private SpaceUserAuthContext getSpaceUserAuthContextByRequest() throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        //兼容POST和GET请求
        if(ContentType.JSON.getValue().equals(contentType)){
            //POST请求
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        }
        else{
            //GET请求
            Map<String, String[]> parameterMap = request.getParameterMap();
            Map<String, String> paramMap = new HashMap<>();
            for (String key : parameterMap.keySet()) {
                    String[] values=parameterMap.get(key);
                    if(values!=null && values.length>0){
                        paramMap.put(key,values[0]);
                    }
            }
            authRequest=BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        Long id = authRequest.getId();
        if (id==null) {
            return authRequest;
        }
        String requestURI = request.getRequestURI();
        String partURI = requestURI.replace(contextPath + "/", "");
        String modelName = StrUtil.subBefore(partURI, "/", false);

        switch (modelName) {
            case "space":
                authRequest.setSpaceId(id);
                break;
            case "picture":
                authRequest.setPictureId(id);
                break;
            case "spaceUser":
                authRequest.setSpaceUserId(id);
                break;
            default: break;
        }
        return authRequest;
    }

    /**
     * 检查对象的所有字段是否都为空
     *
     * @param obj 需要检查的对象
     * @return 如果对象本身为空或所有字段都为空则返回true，否则返回false
     */
    private boolean isAllFieldsNull(Object obj) {
        // 检查对象本身是否为空
        if (ObjUtil.isNull(obj)) {
            return true;//对象本身为空，直接返回true
        }

        // 使用Java 8 Stream API处理对象的字段
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))  // 获取对象的所有字段并转为Stream
                .map(field -> ReflectUtil.getFieldValue(obj, field))  // 获取每个字段的值
                .allMatch(ObjUtil::isNull);  // 检查是否所有字段值都为null
    }

}
