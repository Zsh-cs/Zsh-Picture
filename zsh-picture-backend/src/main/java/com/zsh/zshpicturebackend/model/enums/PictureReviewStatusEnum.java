package com.zsh.zshpicturebackend.model.enums;

import lombok.Getter;

/**
 * 图片审核状态枚举类
 */
@Getter
public enum PictureReviewStatusEnum {

    PENDING_REVIEW("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);

    private final String text;
    private final Integer value;

    PictureReviewStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    // 根据value获取对应的枚举对象
    public static PictureReviewStatusEnum getEnumByValue(Integer value){
        if(value==null){
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if(pictureReviewStatusEnum.value.equals(value)){
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
