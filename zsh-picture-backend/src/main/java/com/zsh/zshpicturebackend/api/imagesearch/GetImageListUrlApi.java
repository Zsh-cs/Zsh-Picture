package com.zsh.zshpicturebackend.api.imagesearch;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 获取相似图片列表地址的API（step 2）
 */
@Slf4j
public class GetImageListUrlApi {

    // 获取相似图片列表地址
    public static String getImageListUrl(String searchResultUrl) {
        try {
            // 1.使用Jsoup获取HTML内容
            Document document = Jsoup.connect(searchResultUrl).timeout(5000).get();
            // 2.获取包含window.cardData的script标签
            Element script = document.select("script:containsData(window.cardData)").first();
            ThrowUtils.throwIf(script == null, ErrorCode.API_ERROR, "未找到包含cardData的script标签");
            // 3.获取script标签内的JavaScript文本
            String scriptText = script.html();
            // 4.提取"windows.cardData = "后面的JSON数组字符串
            String jsonArrayStr = null;
            int start = scriptText.indexOf("window.cardData");
            if (start != -1) {
                int bracketStart = scriptText.indexOf('[', start);
                int bracketEnd = scriptText.lastIndexOf("];");
                if (bracketStart != -1 && bracketEnd != -1) {
                    jsonArrayStr = scriptText.substring(bracketStart, bracketEnd + 1);
                }
            }
            ThrowUtils.throwIf(StrUtil.isBlank(jsonArrayStr), ErrorCode.API_ERROR, "未找到cardData数组");
            // 5.遍历查找cardName为"simipic"的卡片，提取出firstUrl
            JSONArray cardDataArray = JSONUtil.parseArray(jsonArrayStr);
            String firstUrl = null;
            for (Object obj : cardDataArray) {
                JSONObject card = (JSONObject) obj;
                if ("simipic".equals(card.getStr("cardName"))) {
                    JSONObject tplData = card.getJSONObject("tplData");
                    if (tplData != null) {
                        firstUrl = tplData.getStr("firstUrl");
                    }
                    break;
                }
            }
            return firstUrl;
        } catch (Exception e) {
            log.error("获取相似图片列表地址失败", e);
            throw new BusinessException(ErrorCode.API_ERROR, "获取相似图片列表地址失败", e);
        }
    }
}
