package com.zsh.zshpicturebackend.api.ai_text2image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.imagegeneration.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;

import java.util.Collections;

import com.zsh.zshpicturebackend.config.AliyunBailianConfig;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.model.dto.picture.AIText2ImageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * AI文生图API
 */
@Slf4j
@Component
public class AIText2ImageApi {

    // %s填入taskId
    public static final String QUERY_TEXT2IMAGE_TASK_PATH = "/api/v1/tasks/%s";

    @Autowired
    private AliyunBailianConfig aliyunBailianConfig;

    @PostConstruct
    public void init(){
        Constants.baseHttpApiUrl = aliyunBailianConfig.getHost() + "/api/v1";
    }

    // 创建文生图任务
    public ImageGenerationResult createText2ImageTask(AIText2ImageRequest request){
        AIText2ImageRequest.Parameters parameters = request.getParameters();

        ImageGenerationMessage message = ImageGenerationMessage.builder()
                .role("user")
                .content(Collections.singletonList(
                        Collections.singletonMap("text", request.getText())
                )).build();

        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(aliyunBailianConfig.getApiKey())
                .model("wan2.6-t2i")
                .n(parameters.getN())
                .size(parameters.getSize())
                .negativePrompt(parameters.getNegativePrompt())
                .promptExtend(parameters.getPromptExtend())
                .watermark(parameters.getWatermark())
                .messages(Collections.singletonList(message))
                .build();

        ImageGeneration imageGeneration = new ImageGeneration();
        ImageGenerationResult result = null;
        try {
            result = imageGeneration.asyncCall(param);
        } catch (NoApiKeyException | UploadFileException e) {
            throw new RuntimeException(e);
        }

        String errorCode = result.getCode();
        if (StrUtil.isNotBlank(errorCode)) {
            String errorMsg = result.getMessage();
            log.error("创建扩图任务失败，错误码：{}，错误信息：{}", errorCode, errorMsg);
        }
        return result;
    }

    // 根据任务id查询文生图任务
    public QueryText2ImageTaskResponse queryText2ImageTaskByTaskId(String taskId){
        String url = aliyunBailianConfig.getHost() + String.format(QUERY_TEXT2IMAGE_TASK_PATH, taskId);
        HttpRequest httpRequest = HttpRequest.get(url)
                .header("Authorization", "Bearer " + aliyunBailianConfig.getApiKey());
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("HTTP响应不成功，响应体：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.API_ERROR,"HTTP响应不成功");
            }
            QueryText2ImageTaskResponse response = JSONUtil.toBean(httpResponse.body(), QueryText2ImageTaskResponse.class);
            if (StrUtil.isNotBlank(response.getCode())) {
                log.error("查询文生图任务失败，错误码：{}，错误信息：{}", response.getCode(), response.getMessage());
            }
            return response;
        }
    }
}
