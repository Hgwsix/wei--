package com.xiaowei.weipicturebackend.model.dto.picture;

import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunCreateOutPaintingTaskRequest;
import lombok.Data;

@Data
public class CreatePictureOutPaintingRequest {
    private static final long serialVersionUID = 1L;
    /**
     * 图片id
     */
    private Long pictureId;
    /**
     * 扩图参数
     */
    private AliyunCreateOutPaintingTaskRequest.Parameters parameters;
}
