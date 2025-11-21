package com.xiaowei.weipicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunCreateOutPaintingTaskRequest;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunCreateTaskResponse;
import com.xiaowei.weipicturebackend.api.aliyunai.model.AliyunQueryTaskResponse;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class ALiYunAiApi {
    /**
     * 阿里云key
     */
    @Value("${aLiYunAi.aliyunKey}")
    private String aliyunKey;
    /**
     * 创建任务获取任务id的地址
     */
    private static final String CREATE_OUT_PAINTING_TASK_URL="https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    /**
     * 根据任务id获取任务状态地址
     */
    private static final String GET_TASK_STATUS_URL="https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建扩图任务获得任务id
     * @param aliyunCreateOutPaintingTaskRequest
     * @return
     */
    public AliyunCreateTaskResponse createPictureOutPaintingTask(AliyunCreateOutPaintingTaskRequest aliyunCreateOutPaintingTaskRequest){

        System.out.println(JSONUtil.toJsonStr(aliyunCreateOutPaintingTaskRequest));
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.POST, CREATE_OUT_PAINTING_TASK_URL)
                .header("X-DashScope-Async", "enable")
                .header("Authorization", "Bearer " + aliyunKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(aliyunCreateOutPaintingTaskRequest))
                .execute()) {
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI扩图失败");
            }
            AliyunCreateTaskResponse response = JSONUtil.toBean(httpResponse.body(), AliyunCreateTaskResponse.class);
            return response;
        }
    }

    /**
     * 查询创建的任务
     */
    public AliyunQueryTaskResponse getOutPaintingTaskStatus(String taskId){
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR,"任务id不能为空");
        String url=String.format(GET_TASK_STATUS_URL,taskId);
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, url)
                .header("Authorization", "Bearer " + aliyunKey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                log.error("请求异常：", httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI扩图失败");
            }
            AliyunQueryTaskResponse response = JSONUtil.toBean(httpResponse.body(), AliyunQueryTaskResponse.class);
            return response;
        }
    }

}
