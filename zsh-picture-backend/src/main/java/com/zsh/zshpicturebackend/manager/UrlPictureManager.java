package com.zsh.zshpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zsh.zshpicturebackend.constant.SizeConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureManager extends PictureManager{

    private static final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");

    @Override
    protected String verify(Object inputSource) {
        String url=(String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR,"url为空");
        // 校验url格式
        try{
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"url格式不正确");
        }
        // 校验url协议，必须是http或https
        if(!url.startsWith("http://") && !url.startsWith("https://")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"url协议必须是http或https");
        }
        // 校验url长度
        ThrowUtils.throwIf(url.length()>1024,ErrorCode.PARAMS_ERROR,"url长度过长");
        // 发送HEAD请求，验证url图片是否存在
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            // 若没有正常响应，则放弃后续校验直接返回空字符串，而非抛出异常，这样做是为了提高上传url图片的成功率
            // 因为有些url地址可能不支持通过HEAD请求访问，但这不代表对应的网络图片不存在
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return "";
            }
            // 校验图片类型
            String contentType = response.header("Content-Type");
            if(StrUtil.isBlank(contentType)){
                return "";
            } else{
                if (!ALLOW_CONTENT_TYPE.contains(contentType.toLowerCase())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持该图片类型");
                }
                // 校验图片大小
                String contentLengthStr = response.header("Content-Length");
                if(StrUtil.isNotBlank(contentLengthStr)){
                    try {
                        long contentLength = Long.parseLong(contentLengthStr);
                        if(contentLength>2* SizeConstant.ONE_MB){
                            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片大小超过2MB");
                        }
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"发送HEAD请求后响应体的Content-Length格式错误");
                    }
                }
                // 返回时截取掉contentType前面的"image/"部分
                return contentType.substring(6);
            }
        }
    }

    @Override
    protected String getOriginalPictureName(Object inputSource) {
        String url=(String) inputSource;
        // 处理图片url，防止转义或者和COS冲突，同时降低获取不到扩展名的风险
        int questionMarkIndex=url.indexOf('?');
        if(questionMarkIndex>-1){// 说明找到了问号
            url=url.substring(0,questionMarkIndex);
        }
        return url;
    }

    @Override
    protected void transferToTempFile(Object inputSource, File tempFile) {
        String url=(String) inputSource;
        HttpUtil.downloadFile(url,tempFile);
    }
}
