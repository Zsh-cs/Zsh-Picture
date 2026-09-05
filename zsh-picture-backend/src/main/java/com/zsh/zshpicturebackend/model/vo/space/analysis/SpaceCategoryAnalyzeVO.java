package com.zsh.zshpicturebackend.model.vo.space.analysis;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间图片分类分析的响应视图类
 */
@Data
@Builder
public class SpaceCategoryAnalyzeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 该分类下的图片数量
     */
    private Long count;

    /**
     * 该分类下的图片总大小
     */
    private Long totalSize;
}
