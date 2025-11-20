package com.xiaowei.weipicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 创建 DashScope 图像异步任务的响应
 */
@Data
public class AliyunCreateTaskResponse {

    /**
     * 请求 ID
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 任务输出
     */
    private Output output;

    @Data
    public static class Output {

        @Alias("task_status")
        private String taskStatus;

        @Alias("task_id")
        private String taskId;
    }
}

