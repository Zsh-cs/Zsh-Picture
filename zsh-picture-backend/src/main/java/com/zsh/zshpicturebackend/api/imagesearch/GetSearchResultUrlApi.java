package com.zsh.zshpicturebackend.api.imagesearch;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图结果地址的API（step 1）
 */
@Slf4j
public class GetSearchResultUrlApi {

    // 获取以图搜图结果地址
    public static String getSearchResultUrl(String imageUrl) {
        // 1.准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_resource", "PC_UPLOAD_URL");
        String url = "https://graph.baidu.com/upload?uptime=" + System.currentTimeMillis();

        try (// 2.发送POST请求到百度识图接口
             HttpResponse response = HttpRequest.post(url)
                     // 需要指定请求头acs-token，不过随便指定一个数值就行
                     .header("acs-token", RandomUtil.randomString(1))
                     .form(formData).timeout(5000).execute()
        ) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.API_ERROR, "接口响应不成功");
            }
            // 3.解析响应并处理响应结果
            /*{
                "status":0,
                "msg":"Success",
                "data":
                {
                    "url":"https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1&session_id=6011677444107161711&sign=126bf94cff5636307002701787329884&tpl_from=pc",
                    "sign":"126bf94cff5636307002701787329884"
                }
            }*/
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            if (result == null || !result.get("status").equals(0)) {
                throw new BusinessException(ErrorCode.API_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = data.get("url").toString();
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);// 解码
            ThrowUtils.throwIf(StrUtil.isBlank(searchResultUrl), ErrorCode.API_ERROR, "未返回有效结果");
            return searchResultUrl;
        } catch (Exception e) {
            log.error("获取以图搜图结果地址失败", e);
            //! throw的时候带上cause即Exception e，这样控制台才能打印出错误根源
            throw new BusinessException(ErrorCode.API_ERROR, "获取以图搜图结果地址失败", e);
        }
    }
}
