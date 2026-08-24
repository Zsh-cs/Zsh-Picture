package com.zsh.zshpicturebackend.util;

import java.awt.*;

/**
 * 颜色工具类
 */
public class ColorUtil {

    //! 工具类不需要实例化，故将构造器私有化
    private ColorUtil() {
    }

    // 使用欧氏距离算法计算两个颜色的相似度
    public static double calculateSimilarity(Color color1, Color color2) {
        if (color1 == null || color2 == null) {
            throw new IllegalArgumentException("color1 and color2 must not be null");
        }

        double redDiff = color1.getRed() - color2.getRed();
        double greenDiff = color1.getGreen() - color2.getGreen();
        double blueDiff = color1.getBlue() - color2.getBlue();

        double distance = Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
        double maxDistance = Math.sqrt(3 * 255.0 * 255.0);
        return 1.0 - (distance / maxDistance);
    }

}
