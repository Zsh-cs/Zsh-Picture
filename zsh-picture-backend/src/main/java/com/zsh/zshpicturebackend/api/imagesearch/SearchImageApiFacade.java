package com.zsh.zshpicturebackend.api.imagesearch;

import java.util.List;

/**
 * 门面模式，组合三个API，实现高内聚低耦合
 */
public class SearchImageApiFacade {

    // 以图搜图
    public static List<ImageSearchResult> searchImageByImage(String imageUrl) {
        String searchResultUrl = GetSearchResultUrlApi.getSearchResultUrl(imageUrl);
        String imageListUrl = GetImageListUrlApi.getImageListUrl(searchResultUrl);
        return GetImageListApi.getImageList(imageListUrl);
    }
}
