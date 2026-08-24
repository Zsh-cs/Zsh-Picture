package com.zsh.zshpicturebackend.api;

import com.zsh.zshpicturebackend.api.imagesearch.*;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ImageSearchApiTest {

    // 测试获取以图搜图结果地址
    @Test
    public void testGetSearchResultUrlApi() {
        String imageUrl = "https://p1.ssl.qhimg.com/t01ee76fda30f53c3d6.jpg";
        String searchResultUrl = GetSearchResultUrlApi.getSearchResultUrl(imageUrl);
        System.out.println("获取到的以图搜图结果地址：" + searchResultUrl);
    }

    // 测试获取相似图片列表地址
    @Test
    public void testGetImageListUrlApi() {
        String searchResultUrl = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=14267399187046138029&sign=126fa94cff5636307002701787391447&tpl_from=pc";
        String imageListUrl = GetImageListUrlApi.getImageListUrl(searchResultUrl);
        System.out.println("获取到的相似图片列表地址：" + imageListUrl);
    }

    // 测试获取相似图片列表
    @Test
    public void testGetImageListApi() {
        String imageListUrl = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=14267399187046138029&sign=126fa94cff5636307002701787391447&tk=81ed5&tpl_from=pc";
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageListUrl);
        System.out.println("获取到的相似图片列表：" + imageList);
    }

    // 测试整体的以图搜图功能
    @Test
    public void testSearchImageByImage() {
        String imageUrl = "https://p1.ssl.qhimg.com/t01ee76fda30f53c3d6.jpg";
        List<ImageSearchResult> imageList = SearchImageApiFacade.searchImageByImage(imageUrl);
        System.out.println("以图搜图结果：" + imageList);
    }
}
