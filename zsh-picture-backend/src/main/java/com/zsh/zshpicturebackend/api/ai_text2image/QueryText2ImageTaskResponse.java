package com.zsh.zshpicturebackend.api.ai_text2image;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 文生图任务查询响应
 */
@Data
public class QueryText2ImageTaskResponse {

    @Alias("request_id")
    private String requestId;

    private Output output;

    private Usage usage;

    private String code;

    private String message;

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

        private Boolean finished;

        private List<Choice> choices;
    }

    @Data
    public static class Choice {

        @Alias("finish_reason")
        private String finishReason;

        private Message message;
    }

    @Data
    public static class Message {

        private String role;

        private List<Content> content;
    }

    @Data
    public static class Content {

        private String image;

        private String type;
    }

    @Data
    public static class Usage {

        @Alias("image_count")
        private Integer imageCount;

        private String size;

        @Alias("input_tokens")
        private Integer inputTokens;

        @Alias("output_tokens")
        private Integer outputTokens;

        @Alias("total_tokens")
        private Integer totalTokens;
    }
}
