package com.zsh.zshpicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 图片上传的结果
 */
@Data
public class PictureUploadResult {

    /**
     * 图片url
     */
    private String url;

    /**
     * 原图url
     */
    private String originalUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

}
