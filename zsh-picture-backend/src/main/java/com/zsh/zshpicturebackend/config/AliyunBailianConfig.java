package com.zsh.zshpicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼配置
 */
@Configuration
@ConfigurationProperties(prefix = "aliyun.bailian")
@Data
public class AliyunBailianConfig {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 阿里云百炼为华北2（北京）地域推出了业务空间专属域名 https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com
     */
    private String host;

}
