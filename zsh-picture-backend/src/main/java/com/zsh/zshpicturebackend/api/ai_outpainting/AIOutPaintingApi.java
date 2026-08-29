package com.zsh.zshpicturebackend.api.ai_outpainting;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.zsh.zshpicturebackend.config.AliyunBailianConfig;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI扩图API
 */
@Slf4j
@Component
public class AIOutPaintingApi {

    @Autowired
    private AliyunBailianConfig aliyunBailianConfig;

    public static final String CREATE_OUT_PAINTING_TASK_PATH = "/api/v1/services/aigc/image2image/out-painting";
    // %s填的是taskId
    public static final String QUERY_OUT_PAINTING_TASK_PATH = "/api/v1/tasks/%s";

    // 创建扩图任务
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 1.构造请求头和请求体，发送请求
        HttpRequest httpRequest = HttpRequest.post(aliyunBailianConfig.getHost() + CREATE_OUT_PAINTING_TASK_PATH)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aliyunBailianConfig.getApiKey())
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(request));
        // 2.解析响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("HTTP响应不成功，响应体：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.API_ERROR, "HTTP响应不成功");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String code = response.getCode();
            if (StrUtil.isNotBlank(code)) {
                String message = response.getMessage();
                log.error("创建扩图任务失败，错误码：{}，错误信息：{}", code, message);
            }
            return response;
        }
    }

    // 根据任务id查询扩图任务
    public QueryOutPaintingTaskResponse queryOutPaintingTaskByTaskId(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务id不能为空");
        // 1.构造请求url和请求头，发送请求
        String url = aliyunBailianConfig.getHost() + String.format(QUERY_OUT_PAINTING_TASK_PATH, taskId);
        HttpRequest httpRequest = HttpRequest.get(url)
                .header("Authorization", "Bearer " + aliyunBailianConfig.getApiKey());
        // 2.解析响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("HTTP响应不成功，响应体：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.API_ERROR, "HTTP响应不成功");
            }
            QueryOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), QueryOutPaintingTaskResponse.class);
            String code = response.getOutput().getCode();
            if (StrUtil.isNotBlank(code)) {
                String message = response.getOutput().getMessage();
                log.error("查询扩图任务失败，错误码：{}，错误信息：{}", code, message);
            }
            return response;
        }
    }

}
