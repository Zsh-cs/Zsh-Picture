package com.zsh.zshpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.common.DeleteRequest;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.picture.*;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zsh.zshpicturebackend.model.vo.PictureTagCategory;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.service.UserService;
import io.swagger.models.auth.In;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
public class PictureController {

    public static final List<String> TAG_LIST = Arrays.asList("热门", "搞笑", "生活", "高清", "校园", "创意");
    public static final List<String> CATEGORY_LIST = Arrays.asList("摄影", "艺术", "影视", "游戏", "动漫", "表情包");

    @Autowired
    private PictureService pictureService;
    @Autowired
    private UserService userService;

    // 上传本地图片（可以重新上传：基础信息不变，只改变图片文件）
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadLocalPicture(@RequestPart("file") MultipartFile multipartFile,
                                                      PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    // 上传url图片（可以重新上传：基础信息不变，只改变图片文件）
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadUrlPicture(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                    HttpServletRequest request){
        ThrowUtils.throwIf(pictureUploadRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String url = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(url, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    // 删除图片
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        Picture picture = pictureService.getById(id);
        // 判断图片是否存在
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 只有本人或管理员可以删除图片
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean res = pictureService.removeById(id);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 更新图片（仅管理员可用）
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将DTO转换成实体
        Picture newPicture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, newPicture);
        newPicture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 校验图片
        pictureService.verifyPicture(newPicture);
        // 判断id对应图片是否存在
        Picture oldPicture = pictureService.getById(newPicture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 填充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(newPicture,loginUser);
        // 操作数据库
        boolean res = pictureService.updateById(newPicture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 编辑图片（面向用户）
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将DTO转换成实体
        Picture newPicture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, newPicture);
        newPicture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        newPicture.setEditTime(new Date());// 注意要设置编辑时间
        // 校验图片
        pictureService.verifyPicture(newPicture);
        // 判断id对应图片是否存在
        Picture oldPicture = pictureService.getById(newPicture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 只有本人或管理员可以编辑图片
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 填充审核参数
        pictureService.fillReviewParams(newPicture,loginUser);
        // 操作数据库
        boolean res = pictureService.updateById(newPicture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 根据id获取图片（仅管理员可用）
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    // 根据id获取PictureVO对象
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        PictureVO pictureVO = pictureService.obj2vo(picture);
        return ResultUtils.success(pictureVO);
    }

    // 分页获取图片列表（仅管理员可用）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        QueryWrapper<Picture> qw = pictureService.getQueryMapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), qw);
        return ResultUtils.success(picturePage);
    }

    // 分页获取PictureVO列表（有缓存）
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫，防止恶意用户一页展示所有数据然后全部爬走
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "一页展示数据条数过多");
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 分页获取PictureVO对象（有缓存）
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPageWithCache(pictureQueryRequest);
        return ResultUtils.success(pictureVOPage);
    }

    // 获取预制的图片标签及图片分类
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> getPictureTagCategory() {
        PictureTagCategory ptg = new PictureTagCategory();

        ptg.setTagList(TAG_LIST);
        ptg.setCategoryList(CATEGORY_LIST);
        return ResultUtils.success(ptg);
    }

    // 审核图片
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest,
                                               HttpServletRequest request){
        ThrowUtils.throwIf(pictureReviewRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean res = pictureService.reviewPicture(pictureReviewRequest, loginUser);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR,"审核失败");
        return ResultUtils.success(true);
    }

    // 批量抓取并上传url图片（仅管理员可用），返回成功上传的图片数
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadUrlPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                         HttpServletRequest request){
        ThrowUtils.throwIf(pictureUploadByBatchRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadUrlPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

}
