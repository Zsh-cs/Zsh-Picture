package com.zsh.zshpicturebackend.model.enums;

import com.zsh.zshpicturebackend.constant.SizeConstant;
import lombok.Getter;

/**
 * 空间级别枚举
 */
@Getter
public enum SpaceLevelEnum {

    COMMON("普通版", 0, 100L, 100L * SizeConstant.ONE_MB),
    PROFESSIONAL("专业版", 1, 1000L, 1000L * SizeConstant.ONE_MB),
    FLAGSHIP("旗舰版", 2, 10000L, 10000L * SizeConstant.ONE_MB);

    private final String text;
    private final Integer value;
    private final long maxCount;// 最大图片总数量
    private final long maxSize;// 最大图片总大小

    SpaceLevelEnum(String text, Integer value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.value.equals(value)) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
