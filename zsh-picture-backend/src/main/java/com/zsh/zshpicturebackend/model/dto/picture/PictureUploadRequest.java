package com.zsh.zshpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 * 需要支持重新上传：基础信息不变，只改变图片文件
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id（用于更新图片）
     */
    private Long id;

    /**
     * 网络图片的url
     */
    private String fileUrl;
}
