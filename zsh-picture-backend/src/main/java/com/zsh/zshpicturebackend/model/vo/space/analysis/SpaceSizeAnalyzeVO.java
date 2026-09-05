package com.zsh.zshpicturebackend.model.vo.space.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片大小分析的响应视图类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceSizeAnalyzeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片大小范围
     */
    private String sizeRange;

    /**
     * 图片数量
     */
    private Long count;
}
