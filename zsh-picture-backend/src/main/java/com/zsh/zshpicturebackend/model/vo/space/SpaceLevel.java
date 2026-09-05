package com.zsh.zshpicturebackend.model.vo.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    private String text;
    private Integer value;
    private long maxCount;
    private long maxSize;
}
