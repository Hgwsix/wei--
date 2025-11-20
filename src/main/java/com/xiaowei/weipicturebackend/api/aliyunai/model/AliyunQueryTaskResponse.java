package com.xiaowei.weipicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 查询 DashScope 异步任务的响应
 */
@Data
public class AliyunQueryTaskResponse {

    @Alias("request_id")
    private String requestId;

    private Output output;

    private Usage usage;

    @Data
    public static class Output {

        @Alias("task_id")
        private String taskId;

        @Alias("task_status")
        private String taskStatus;

        @Alias("submit_time")
        private String submitTime;

        @Alias("scheduled_time")
        private String scheduledTime;

        @Alias("end_time")
        private String endTime;

        @Alias("output_image_url")
        private String outputImageUrl;
    }

    @Data
    public static class Usage {

        @Alias("image_count")
        private Integer imageCount;
    }
}

