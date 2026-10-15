package com.xiaowei.weipicturebackend.manager.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xiaowei.weipicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.xiaowei.weipicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.xiaowei.weipicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.xiaowei.weipicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.xiaowei.weipicturebackend.model.entity.User;
import com.xiaowei.weipicturebackend.model.vo.UserVO;
import com.xiaowei.weipicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 在连接成功，也就是握手成功之后需要做的事情
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //保存会话到会话集合中
        Long pictureId = (Long)session.getAttributes().get("pictureId");
        User user = (User)session.getAttributes().get("user");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        //构造响应
        PictureEditResponseMessage  pictureEditResponseMessage = new PictureEditResponseMessage();
        UserVO userVO=userService.getUserVO(user);
        pictureEditResponseMessage.setUser(userVO);
        String message =String.format("%s加入编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        //广播给同一张图片的用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }
    /**
     * 处理从客户端接收到的信息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //解析成编辑请求
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);

        //从会话中获取信息
        Long pictureId = (Long)session.getAttributes().get("pictureId");
        User user = (User)session.getAttributes().get("user");

        //根据类型处理请求
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT -> {
                handleEnterEditMessage(session,pictureEditRequestMessage,pictureId, user);
                break;
            }
            case EXIT_EDIT -> {
                handleExitEditMessage(session,pictureEditRequestMessage,pictureId, user);
                break;
            }
            case EDIT_ACTION -> {
                handleEditActionMessage(session,pictureEditRequestMessage,pictureId, user);
                break;
            }
            default -> {
                PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("编辑消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
            }
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //从会话集合中移除
        Long pictureId = (Long)session.getAttributes().get("pictureId");
        User user = (User)session.getAttributes().get("user");
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            webSocketSessions.remove(session);
            if(CollUtil.isEmpty(webSocketSessions)){
                pictureSessions.remove(pictureId);
            }
        }
        //删除编辑状态
        handleExitEditMessage(session, null, pictureId, user);
        //构造响应
        PictureEditResponseMessage  pictureEditResponseMessage = new PictureEditResponseMessage();
        UserVO userVO=userService.getUserVO(user);
        pictureEditResponseMessage.setUser(userVO);
        String message =String.format("%s离开编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 执行编辑操作
     * @param session
     * @param pictureEditRequestMessage
     * @param pictureId
     * @param user
     */
    private void handleEditActionMessage(WebSocketSession session, PictureEditRequestMessage pictureEditRequestMessage, Long pictureId, User user) throws Exception {
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.valueOf(editAction);
        if (pictureEditActionEnum == null) {
            return;
        }
        //只有当前的的用户是编辑者才可以进行编辑
        Long editUserId = pictureEditingUsers.get(pictureId);
        if(editUserId != null && editUserId.equals(user.getId())){
            //构造响应
            PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行了%s", user.getUserName(), pictureEditActionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            //广播给其他会话
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 退出当前的编辑状态
     * @param session
     * @param pictureEditRequestMessage
     * @param pictureId
     * @param user
     */
    private void handleExitEditMessage(WebSocketSession session, PictureEditRequestMessage pictureEditRequestMessage, Long pictureId, User user) throws Exception {

        Long editUserId = pictureEditingUsers.get(pictureId);
        if(editUserId != null && editUserId.equals(user.getId())){
            //退出编辑状态
            pictureEditingUsers.remove(pictureId);
            //构造响应并且广播给其他会话
            PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message=String.format("%s退出编辑",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);

        }
    }

    /**
     * 进入编辑状态
     * @param session
     * @param pictureEditRequestMessage
     * @param pictureId
     * @param user
     */
    private void handleEnterEditMessage(WebSocketSession session, PictureEditRequestMessage pictureEditRequestMessage, Long pictureId, User user) throws Exception {
        //没有用户在编辑图片时才可以进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            //设置当前用户进入编辑状态
            pictureEditingUsers.put(pictureId, user.getId());
            //构造响应
            PictureEditResponseMessage pictureEditResponseMessage=new PictureEditResponseMessage();
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            String message =String.format("%s开始编辑图片",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            //广播到其他会话
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 给除了自己以外的会话广播
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @throws Exception
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws Exception {
        //获取对应图片的所有会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if(CollUtil.isNotEmpty(webSocketSessions)){
            ObjectMapper objectMapper = new ObjectMapper();
            // 创建SimpleModule
            SimpleModule simpleModule = new SimpleModule();
            // Long类型转String（包括long基本类型）
            simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
            simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // 注册模块
            objectMapper.registerModule(simpleModule);
            //序列化为JSON字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage= new TextMessage(message);
            for (WebSocketSession session : webSocketSessions) {
                if(session.equals(excludeSession)) {
                    continue;
                }
                if(session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 给所有会话广播
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
         broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
