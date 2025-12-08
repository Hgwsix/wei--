package com.xiaowei.weipicturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型如："ENTER_EDIT" "EXIT_EDIT" "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑操作
     */
    private String editAction;
}
