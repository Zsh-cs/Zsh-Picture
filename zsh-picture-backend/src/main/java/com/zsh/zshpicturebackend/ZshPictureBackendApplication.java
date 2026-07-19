package com.zsh.zshpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.zsh.zshpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZshPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZshPictureBackendApplication.class, args);
    }

}
