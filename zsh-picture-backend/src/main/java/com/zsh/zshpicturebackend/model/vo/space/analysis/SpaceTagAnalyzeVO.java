package com.zsh.zshpicturebackend.model.vo.space.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片标签分析的响应视图类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTagAnalyzeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 该标签关联的图片数量
     */
    private Long count;
}
