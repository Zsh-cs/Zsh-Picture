package com.zsh.zshpicturebackend.api.ai_outpainting;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 根据任务id查询扩图任务的响应
 */
@Data
public class QueryOutPaintingTaskResponse {

    /**
     * 请求唯一标识，可用于请求明细溯源和问题排查
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 输出的任务信息
     */
    private Output output;

    /**
     * 图像统计信息
     */
    private Usage usage;

    /**
     * 输出的任务信息
     */
    @Data
    public static class Output {

        /**
         * 任务ID，查询有效期24小时
         */
        @Alias("task_id")
        private String taskId;

        /**
         * 任务状态
         * PENDING：任务排队中
         * RUNNING：任务处理中
         * SUCCEEDED：任务执行成功
         * FAILED：任务执行失败
         * CANCELED：任务已取消
         * UNKNOWN：任务不存在或状态未知
         */
        @Alias("task_status")
        private String taskStatus;

        /**
         * 任务结果统计
         */
        @Alias("task_metrics")
        private TaskMetrics taskMetrics;

        /**
         * 任务提交时间
         * 格式：YYYY-MM-DD HH.mm.ss.SSS
         */
        @Alias("submit_time")
        private String submitTime;

        /**
         * 任务调度时间
         * 格式：YYYY-MM-DD HH.mm.ss.SSS
         */
        @Alias("scheduled_time")
        private String scheduledTime;

        /**
         * 任务完成时间
         * 格式：YYYY-MM-DD HH.mm.ss.SSS
         */
        @Alias("end_time")
        private String endTime;

        /**
         * 输出图像URL地址
         */
        @Alias("output_image_url")
        private String outputImageUrl;

        /**
         * 请求失败的错误码（成功时不返回）
         */
        private String code;

        /**
         * 请求失败的详细信息（成功时不返回）
         */
        private String message;
    }

    /**
     * 任务结果统计
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总的任务数
         */
        private Integer total;

        /**
         * 任务状态为成功的任务数
         */
        private Integer succeeded;

        /**
         * 任务状态为失败的任务数
         */
        private Integer failed;
    }

    /**
     * 图像统计信息
     */
    @Data
    public static class Usage {

        /**
         * 模型成功生成图片的数量
         * 计费公式：费用 = 图片数量 × 单价
         */
        @Alias("image_count")
        private Integer imageCount;
    }
}