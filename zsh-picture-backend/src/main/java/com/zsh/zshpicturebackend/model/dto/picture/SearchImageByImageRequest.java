package com.zsh.zshpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchImageByImageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long pictureId;
}
