package com.zsh.zshpicturebackend.model.dto.space.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间使用排行分析请求
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 查询排名前N的空间，默认排名前十
     */
    private Integer topN=10;
}
