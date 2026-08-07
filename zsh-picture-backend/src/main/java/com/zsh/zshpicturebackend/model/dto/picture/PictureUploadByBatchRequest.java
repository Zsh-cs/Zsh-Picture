package com.zsh.zshpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取和上传图片的请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量，默认是10条
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
