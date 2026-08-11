package com.zsh.zshpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.manager.LocalPictureManager;
import com.zsh.zshpicturebackend.manager.PictureManager;
import com.zsh.zshpicturebackend.manager.UrlPictureManager;
import com.zsh.zshpicturebackend.model.dto.picture.*;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.mapper.PictureMapper;
import com.zsh.zshpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author asus
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-07-26 17:36:13
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Autowired
    private LocalPictureManager localPictureManager;
    @Autowired
    private UrlPictureManager urlPictureManager;
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 基础的缓存过期时间（单位：秒）
    public static final int BASIC_CACHE_EXPIRE_TIME = 30;

    // 本地缓存
    public static final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)// 最多存储10000条数据
            .expireAfterWrite(Duration.ofSeconds(BASIC_CACHE_EXPIRE_TIME))
            .build();

    // 上传图片（本地图片或url图片）
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {

        // 1.校验参数
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"未登录用户不可以上传图片");
        // 默认是新增图片，所以pictureId为空
        Long pictureId=null;
        // 若图片上传请求不为空，则是更新图片，需要为pictureId赋值
        if(pictureUploadRequest !=null){
            pictureId= pictureUploadRequest.getId();
        }
        // 如果是更新图片，要去数据库查询pictureId对应的图片是否存在
        if(pictureId!=null){
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
            // 图片存在，则只能允许本人或管理员更新
            if(!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 2.上传图片
        // 目前先上传到公共空间，路径前缀为public/用户id，这样可以区分不同用户上传的图片
        String uploadPathPrefix=String.format("public/%s",loginUser.getId());
        // 根据输入源的类型调用不同的Manager上传图片
        PictureManager pictureManager=localPictureManager;
        if(inputSource instanceof String){
            pictureManager=urlPictureManager;
        }
        PictureUploadResult pictureUploadResult = pictureManager.uploadPicture(inputSource, uploadPathPrefix);

        // 3.操作数据库
        // 构造要入库的图片信息
        Picture picture=new Picture();
        BeanUtils.copyProperties(pictureUploadResult,picture);
        // 支持外部传递图片名称
        if(pictureUploadRequest!=null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            picture.setName(pictureUploadRequest.getPicName());
        }
        picture.setUserId(loginUser.getId());
        // 填充审核参数
        fillReviewParams(picture,loginUser);
        // 如果是更新图片，需要补充pictureId和编辑时间
        if(pictureId!=null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 根据图片id进行判断，存在则更新图片，否则新增图片
        boolean res = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);

        // 获取新增或更新后数据库中的图片对象，因为它包含了createTime、editTime、updateTime
        Picture pictureInDB = this.getById(picture.getId());// 主键回填
        return obj2vo(pictureInDB);
    }

    // Picture转PictureVO
    @Override
    public PictureVO obj2vo(Picture picture) {
        PictureVO pictureVO = obj2incompleteVO(picture);
        Long userId = picture.getUserId();
        if(userId!=null && userId>0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    // Picture转暂不包含UserVO的PictureVO
    @Override
    public PictureVO obj2incompleteVO(Picture picture){
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }

    // PictureVO转Picture
    @Override
    public Picture vo2obj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    // 将查询请求转化为QueryMapper对象
    @Override
    public QueryWrapper<Picture> getQueryMapper(PictureQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = request.getId();
        String name = request.getName();
        String introduction = request.getIntroduction();
        String category = request.getCategory();
        List<String> tags = request.getTags();
        Long picSize = request.getPicSize();
        Integer picWidth = request.getPicWidth();
        Integer picHeight = request.getPicHeight();
        Double picScale = request.getPicScale();
        String picFormat = request.getPicFormat();
        String searchText = request.getSearchText();
        Long userId = request.getUserId();
        Integer reviewStatus = request.getReviewStatus();
        String reviewMessage = request.getReviewMessage();
        Long reviewerId = request.getReviewerId();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();

        QueryWrapper<Picture> qw=new QueryWrapper<>();
        qw.eq(ObjUtil.isNotEmpty(id),"id",id)
                .like(StrUtil.isNotBlank(name),"name",name)
                .like(StrUtil.isNotBlank(introduction),"introduction",introduction)
                .eq(StrUtil.isNotBlank(category),"category",category);
        // 使用like查询来匹配tags字符串中的某一个tag，是相对比较简单的一种方法
        // 示例：tags="["Java","Python","C++"]"
        // where ... and (tags like "%\"Java\"%" and tags like "%\"Python\"%" and ...) ...
        if(CollUtil.isNotEmpty(tags)){
            for (String tag : tags) {
                qw.and(i->i.like("tags","\""+tag+"\""));
            }
        }
        qw.eq(ObjUtil.isNotEmpty(picSize),"picSize",picSize)
                .eq(ObjUtil.isNotEmpty(picWidth),"picWidth",picWidth)
                .eq(ObjUtil.isNotEmpty(picHeight),"picHeight",picHeight)
                .eq(ObjUtil.isNotEmpty(picScale),"picScale",picScale)
                .eq(StrUtil.isNotBlank(picFormat),"picFormat",picFormat);
        // where ... and (name like "%xxx%" or introduction like "%xxx%") ...
        if(StrUtil.isNotBlank(searchText)){
            qw.and(i->i.like("name",searchText).or().like("introduction",searchText));
        }
        qw.eq(ObjUtil.isNotEmpty(userId),"userId",userId)
                .eq(ObjUtil.isNotEmpty(reviewStatus),"reviewStatus",reviewStatus)
                .eq(StrUtil.isNotBlank(reviewMessage),"reviewMessage",reviewMessage)
                .eq(ObjUtil.isNotEmpty(reviewerId),"reviewId",reviewerId)
                .orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);

        return qw;
    }

    // 分页获取PictureVO对象
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage){
        // 如果遍历每一个picture，都调用一次obj2vo()方法，会产生大量数据库操作，性能极低
        // 因此我们的思路是：先获取到要查询的用户id列表，只进行一次查询用户表的数据库操作
        // 再将查到的用户分别设置到对应的PictureVO对象中，这样可以大大提高性能
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage=new Page<>(picturePage.getCurrent(), picturePage.getSize(),picturePage.getTotal());
        if(CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }

        // 将实体对象列表转换成VO对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(this::obj2incompleteVO).collect(Collectors.toList());
        // 获取到要查询的用户id列表，使用Set是因为Set不包含重复的用户id
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 到数据库中根据这份用户id列表查出对应的所有用户（只需查询一次）
        List<User> userList = userService.listByIds(userIdSet);
        // 构造一个map，key是userId，value是对应的user
        Map<Long, User> map = userList.stream().collect(Collectors.toMap(User::getId, user -> user));
        // 将查到的用户分别设置到对应的PictureVO对象中
        for (PictureVO pictureVO : pictureVOList) {
            Long userId = pictureVO.getUserId();
            User user=null;
            if(map.containsKey(userId)){
                user=map.get(userId);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        }
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    // 分页获取PictureVO对象（有缓存）
    //? 多级缓存：本地Caffeine缓存->Redis缓存->数据库
    @Override
    public Page<PictureVO> getPictureVOPageWithCache(PictureQueryRequest pictureQueryRequest) {

        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        Page<PictureVO> pictureVOPage;
        // 1.构建key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String key = "zsh_picture:listPictureVOByPage:" + hashKey;
        // 2.先查本地缓存，命中直接返回
        String value = LOCAL_CACHE.getIfPresent(key);
        if (value != null) {
            pictureVOPage = JSONUtil.toBean(value, Page.class);
        } else {
            // 3.本地缓存不命中，则查Redis缓存，命中直接返回，同时把Redis缓存的数据存入本地缓存
            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            value = ops.get(key);
            if (value != null) {
                pictureVOPage = JSONUtil.toBean(value, Page.class);
                LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVOPage));
            } else {
                // 4.Redis缓存不命中，则查数据库，然后把查到的数据存入本地缓存和Redis缓存
                QueryWrapper<Picture> qw = this.getQueryMapper(pictureQueryRequest);
                Page<Picture> picturePage = this.page(new Page<>(current, pageSize), qw);
                pictureVOPage = this.getPictureVOPage(picturePage);
                // 存入本地缓存
                LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVOPage));
                // 存入Redis缓存，随机过期，防止缓存雪崩
                int cacheExpireTime = BASIC_CACHE_EXPIRE_TIME + RandomUtil.randomInt(0, BASIC_CACHE_EXPIRE_TIME);
                ops.set(key, JSONUtil.toJsonStr(pictureVOPage), cacheExpireTime, TimeUnit.SECONDS);
            }
        }
        return pictureVOPage;
    }

    // 校验图片
    @Override
    public void verifyPicture(Picture picture){
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        // 校验id、url（如果用户有传过来）和图片简介（如果用户有传过来）
        if(picture.getId()==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片id为空");
        }
        String url = picture.getUrl();
        if(StrUtil.isNotBlank(url) && url.length()>1024){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片url过长");
        }
        String introduction = picture.getIntroduction();
        if(StrUtil.isNotBlank(introduction) && introduction.length()>800){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片简介过多");
        }
    }

    // 审核图片
    @Override
    public boolean reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1.校验参数，审核状态不能为“待审核”（因为“待审核”就相当于没有进行审核操作）
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if(id == null || reviewStatusEnum==null || PictureReviewStatusEnum.PENDING_REVIEW.equals(reviewStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.判断图片是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR);
        // 3.避免重复审核
        if(reviewStatus.equals(picture.getReviewStatus())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该图片已经审核过了");
        }
        // 4.更新审核状态
        Picture pictureAfterReview=new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,pictureAfterReview);
        pictureAfterReview.setReviewerId(loginUser.getId());
        pictureAfterReview.setReviewTime(new Date());
        return this.updateById(pictureAfterReview);
    }

    /**
     * 填充审核参数
     * 1.管理员上传或更新图片时，自动过审并且填充审核参数
     * 2.用户上传或编辑图片时，图片状态一律重置为“待审核”
     *
     * @param picture   上传/用户编辑/管理员更新的图片
     * @param loginUser 当前登录的用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if(userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else{
            picture.setReviewStatus(PictureReviewStatusEnum.PENDING_REVIEW.getValue());
        }
    }

    // 批量抓取和上传url图片，返回成功上传的图片数
    @Override
    public int uploadUrlPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 1.校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "每次最多抓取30张图片");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        // 如果用户传入的名称前缀为空，就默认等于搜索词
        if(StrUtil.isBlank(namePrefix)){
            namePrefix=searchText;
        }

        // 2.抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("jsoup获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "jsoup获取页面失败");
        }

        // 3.解析内容
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isEmpty(div), ErrorCode.OPERATION_ERROR, "jsoup获取元素失败");
        Elements imgElements = div.select("img.mimg");
        ThrowUtils.throwIf(CollUtil.isEmpty(imgElements), ErrorCode.OPERATION_ERROR, "jsoup获取元素失败");

        // 4.遍历元素并上传图片
        int uploadCount = 0;// 成功上传的图片数
        for (int i = 0; i < imgElements.size(); i++) {
            Element imgElement = imgElements.get(i);
            String imgUrl = imgElement.attr("src");
            if (StrUtil.isBlank(imgUrl)) {
                log.warn("第{}张图片的url不存在，抓取失败，已跳过", i+1);
                continue;
            }
            // 处理图片url，防止转义或者和COS冲突
            // 比如这个url：https://tse4-mm.cn.bing.net/th/id/OIP-C.SJGV7f_dRfnt_tPF_JGmXgHaE8?w=300&h=200&c=7&r=0&o=7&pid=1.7&rm=3
            // 问号后面的参数都是不必要的，可以通通删掉，变成https://tse4-mm.cn.bing.net/th/id/OIP-C.SJGV7f_dRfnt_tPF_JGmXgHaE8
            int indexOfQuestionMark = imgUrl.indexOf('?');
            if (indexOfQuestionMark > -1) {// 说明找到了问号
                imgUrl = imgUrl.substring(0, indexOfQuestionMark);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(imgUrl);
            pictureUploadRequest.setPicName(namePrefix +(i+1));
            try{
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，图片id={}",pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败",e);
                continue;// 跳过，继续抓取并上传下一张图片
            }
            // 若成功上传的图片数uploadCount已经超过需要抓取并上传的图片数count，则停止后续操作
            if(uploadCount>=count){
                break;
            }
        }

        return uploadCount;
    }

}




