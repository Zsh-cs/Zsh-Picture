package com.zsh.zshpicturebackend.api.ai_outpainting;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务的响应
 */
@Data
public class CreateOutPaintingTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务输出信息
     */
    private Output output;

    /**
     * 请求唯一标识，可用于请求明细溯源和问题排查
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 请求失败的错误码，请求成功时不会返回此参数
     */
    private String code;

    /**
     * 请求失败的详细信息，请求成功时不会返回此参数
     */
    private String message;

    /**
     * 任务输出信息
     */
    @Data
    public static class Output implements Serializable {

        private static final long serialVersionUID = 1L;

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

    }
}
