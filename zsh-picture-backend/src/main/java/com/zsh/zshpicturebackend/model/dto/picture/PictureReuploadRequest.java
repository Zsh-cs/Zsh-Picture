package com.zsh.zshpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片重新上传请求：基础信息不变，只改变图片文件
 */
@Data
public class PictureReuploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id（用于更新图片）
     */
    private Long pictureId;
}
