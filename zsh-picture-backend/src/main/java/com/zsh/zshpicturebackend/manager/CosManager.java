package com.zsh.zshpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zsh.zshpicturebackend.config.CosClientConfig;
import com.zsh.zshpicturebackend.constant.SizeConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Autowired
    private CosClientConfig cosClientConfig;
    @Autowired
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        return cosClient.putObject(cosClientConfig.getBucket(), key, file);
    }

    // 下载对象
    public COSObject getObject(String key) {
        return cosClient.getObject(cosClientConfig.getBucket(), key);
    }

    // 删除对象
    public void deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 上传并解析图片
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        //! 以下操作独立于把原图存入COS
        PicOperations picOperations = new PicOperations();
        // 1表示返回原图信息
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1.压缩图规则
        PicOperations.Rule compressRule = new PicOperations.Rule();
        // FileId不以“/”开头为相对路径，压缩后的图片会保存在与原图相同的目录中
        compressRule.setFileId(FileUtil.mainName(key) + ".webp");
        // 转换成webp格式，质量变换为相对原图的90%
        compressRule.setRule("imageMogr2/format/webp/rquality/90");
        compressRule.setBucket(cosClientConfig.getBucket());
        rules.add(compressRule);
        // 2.缩略图规则：原图>20KB才缩略，不然缩略图比压缩图还大
        if(file.length()>20* SizeConstant.ONE_KB){
            PicOperations.Rule thumbnailRule=new PicOperations.Rule();
            thumbnailRule.setFileId(FileUtil.mainName(key)+"_thumbnail."+FileUtil.getSuffix(key));
            // /thumbnail/<Width>x<Height>>
            // 限定缩略图的宽度和高度的最大值分别为 Width 和 Height，进行等比缩小，缩放比例取宽缩放比和高缩放比的较小值
            // 如果目标宽（高）大于原图宽（高），则不处理
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",256,256));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            rules.add(thumbnailRule);
        }

        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    // 根据url删除图片
    public void deletePictureByUrl(String url){
        String host = cosClientConfig.getHost()+"/";
        String key = url.substring(host.length());
        deleteObject(key);
    }
}
