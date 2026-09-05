package com.zsh.zshpicturebackend.model.dto.space.analysis;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分析请求
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 是否查询全部空间
     */
    private boolean queryAll;


}
