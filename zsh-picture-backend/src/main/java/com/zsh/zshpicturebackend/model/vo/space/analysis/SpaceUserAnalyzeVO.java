package com.zsh.zshpicturebackend.model.vo.space.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间用户上传行为分析的响应视图类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceUserAnalyzeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间区间
     */
    private String period;

    /**
     * 用户上传的图片数量
     */
    private Long count;
}
