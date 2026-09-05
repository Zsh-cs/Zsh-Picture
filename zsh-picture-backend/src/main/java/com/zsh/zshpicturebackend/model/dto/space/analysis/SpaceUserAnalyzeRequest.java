package com.zsh.zshpicturebackend.model.dto.space.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户上传行为分析请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 时间维度：day/week/month
     */
    private String timeDimension;
}
