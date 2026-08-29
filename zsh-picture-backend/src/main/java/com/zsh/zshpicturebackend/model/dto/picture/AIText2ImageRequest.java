package com.zsh.zshpicturebackend.model.dto.picture;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图像生成请求体
 */
@Data
public class AIText2ImageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 输入的文本
     */
    private String text;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    /**
     * 图像处理参数
     */
    @Data
    public static class Parameters implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 反向提示词，用于描述不希望在图像中出现的内容，对画面进行限制。
         * 支持中英文，长度不超过500个字符，超出部分自动截断。
         */
        @Alias("negative_prompt")
        private String negativePrompt;

        /**
         * 输出图像的分辨率，格式为 "宽*高"，默认值为 "1280*1280"。
         * 总像素在 [1280*1280, 1440*1440] 之间且宽高比范围为 [1:4, 4:1]。
         * 常见比例推荐：
         * <ul>
         *   <li>1:1  -> 1280*1280</li>
         *   <li>3:4  -> 1104*1472</li>
         *   <li>4:3  -> 1472*1104</li>
         *   <li>9:16 -> 960*1696</li>
         *   <li>16:9 -> 1696*960</li>
         * </ul>
         */
        private String size;

        /**
         * 生成图片的数量，取值范围 1~4，默认为 4。
         * 注意：按张计费，费用 = 单价 × 图片张数。
         */
        private Integer n;

        /**
         * 是否开启prompt智能改写。
         * 开启后将使用大模型优化正向提示词，对较短的提示词有明显提升效果，但增加3-4秒耗时。
         * true（默认）：开启；false：不开启。
         * 注意：开启后可能引入受版权保护的内容，触发 IPInfringementSuspect 或 DataInspectionFailed 报错，
         * 遇到该报错时可设为 false 重试。
         */
        @Alias("prompt_extend")
        private Boolean promptExtend;

        /**
         * 是否添加水印标识，水印位于图片右下角，文案固定为"AI生成"。
         * false（默认）：不添加；true：添加。
         */
        private Boolean watermark;

    }
}