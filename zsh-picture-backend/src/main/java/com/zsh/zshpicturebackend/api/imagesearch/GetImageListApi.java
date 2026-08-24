package com.zsh.zshpicturebackend.api.imagesearch;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.JsonArray;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 获取相似图片列表的API（step 3）
 */
@Slf4j
public class GetImageListApi {

    // 获取相似图片列表，提取出需要的东西封装成List<ImageSearchResult>并返回
    public static List<ImageSearchResult> getImageList(String imageListUrl) {
        try (// 1.发送GET请求
             HttpResponse response = HttpRequest.get(imageListUrl).timeout(5000).execute();
        ) {
            // 2.获取响应内容
            int status = response.getStatus();
            String body = response.body();
            ThrowUtils.throwIf(status != HttpStatus.HTTP_OK, ErrorCode.API_ERROR, "接口响应不成功");
            // 3.解析响应
            JSONObject jsonObject = new JSONObject(body);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            return JSONUtil.toList(list, ImageSearchResult.class);
        } catch (Exception e) {
            log.error("获取相似图片列表失败", e);
            throw new BusinessException(ErrorCode.API_ERROR, "获取相似图片列表失败", e);
        }
    }
}
