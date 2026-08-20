package com.zsh.zshpicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
}
