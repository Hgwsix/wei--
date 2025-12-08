package com.xiaowei.weipicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaowei.weipicturebackend.enums.SpaceTypeEnums;
import com.xiaowei.weipicturebackend.manager.auth.SpaceUserAuthManager;
import com.xiaowei.weipicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xiaowei.weipicturebackend.model.entity.Picture;
import com.xiaowei.weipicturebackend.model.entity.Space;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.service.PictureService;
import com.xiaowei.weipicturebackend.service.SpaceService;
import com.xiaowei.weipicturebackend.service.SpaceUserService;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,@NonNull Map<String, Object> attributes) throws Exception {
        if(request instanceof ServletServerHttpRequest){
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if(StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            //检查登入
            User loginUser = userService.getLoginUser(servletRequest);
            if(ObjUtil.isNull(loginUser)){
                log.error("未登入，拒绝握手");
                return false;
            }
            //检查图片是否存在
            Picture picture = pictureService.getById(Long.parseLong(pictureId));
            if(picture==null){
                log.error("图片不存在，拒绝握手");
                return false;
            }
            //校验是否有编辑图片的权限
            Long spaceId = picture.getSpaceId();
            if(spaceId==null){
                //公共图库，不能协同编辑
                return false;
            }
            //检查空间是否存在
            Space space = spaceService.getById(spaceId);
            if(space==null){
                log.error("空间不存在，拒绝握手");
                return false;
            }
            //不是团队空间，不能协同编辑
            if(!space.getSpaceType().equals(SpaceTypeEnums.TEAM.getValue())){
                log.error("不是团队空间，拒绝握手");
                return false;
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.error("没有编辑图片的权限，拒绝握手");
                return false;
            }
            //设置attributes
            attributes.put("userId", loginUser.getId());
            attributes.put("user",loginUser);
            attributes.put("pictureId", Long.valueOf(pictureId));

        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
