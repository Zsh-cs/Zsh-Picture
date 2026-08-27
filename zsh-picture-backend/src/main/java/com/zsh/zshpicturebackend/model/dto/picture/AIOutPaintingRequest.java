package com.zsh.zshpicturebackend.model.dto.picture;

import com.zsh.zshpicturebackend.api.ai_outpainting.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * AI扩图请求
 */
@Data
public class AIOutPaintingRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
