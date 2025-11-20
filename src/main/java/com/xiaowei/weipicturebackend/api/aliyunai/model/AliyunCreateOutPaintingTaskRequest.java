package com.xiaowei.weipicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 请求 DashScope 异步图像扩展任务的入参
 */
@Data
public class AliyunCreateOutPaintingTaskRequest {

    /**
     * 模型名称，如：image-out-painting
     */
    private String model="image-out-painting";

    /**
     * 输入配置
     */
    private Input input;

    /**
     * 扩图参数
     */
    private Parameters parameters;

    @Data
    public static class Input {

        /**
         * 原图地址
         */
        @Alias("image_url")
        private String imageUrl;
    }

    @Data
    public static class Parameters {

        /**
         * 横向拉伸比例
         */
        @Alias("x_scale")
        @JsonProperty("xScale")
        private Double xScale;

        /**
         * 纵向拉伸比例
         */
        @Alias("y_scale")
        @JsonProperty("yScale")
        private Double yScale;

        /**
         * 可选，在图像上方添加像素，默认值 0
         */
        @Alias("top_offset")
        private Integer topOffset;

        /**
         * 可选，在图像下方添加像素，默认值 0
         */
        @Alias("bottom_offset")
        private Integer bottomOffset;

        /**
         * 可选，在图像左侧添加像素，默认值 0
         */
        @Alias("left_offset")
        private Integer leftOffset;

        /**
         * 可选，在图像右侧添加像素，默认值 0
         */
        @Alias("right_offset")
        private Integer rightOffset;

        /**
         * 可选，按照角度扩展画布
         */
        @Alias("angle")
        private Double angle;

        /**
         * 可选，是否限制输出图像大小
         */
        @Alias("limit_image_size")
        private Boolean limitImageSize;

        /**
         * 可选，输出图像宽高比
         */
        @Alias("output_ratio")
        private String outputRatio;

        /**
         * 可选，是否开启最佳质量
         */
        @Alias("best_quality")
        private Boolean bestQuality;

        /**
         * 可选，是否添加水印
         */
        @Alias("add_watermark")
        private Boolean addWatermark;

    }
}

