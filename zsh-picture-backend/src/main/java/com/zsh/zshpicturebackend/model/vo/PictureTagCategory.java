package com.zsh.zshpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签及图片分类的VO
 */
@Data
public class PictureTagCategory {

    /**
     * 图片标签列表
     */
    private List<String> tagList;

    /**
     * 图片分类列表
     */
    private List<String> categoryList;
}
