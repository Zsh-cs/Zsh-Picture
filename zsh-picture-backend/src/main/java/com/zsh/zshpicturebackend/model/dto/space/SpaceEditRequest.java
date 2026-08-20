package com.zsh.zshpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间编辑请求，给用户使用
 */
@Data
public class SpaceEditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;
}
