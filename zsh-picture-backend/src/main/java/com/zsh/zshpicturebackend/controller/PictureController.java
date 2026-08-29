package com.zsh.zshpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsh.zshpicturebackend.annotation.AuthCheck;
import com.zsh.zshpicturebackend.api.ai_outpainting.AIOutPaintingApi;
import com.zsh.zshpicturebackend.api.ai_outpainting.CreateOutPaintingTaskResponse;
import com.zsh.zshpicturebackend.api.ai_outpainting.QueryOutPaintingTaskResponse;
import com.zsh.zshpicturebackend.api.ai_text2image.AIText2ImageApi;
import com.zsh.zshpicturebackend.api.ai_text2image.QueryText2ImageTaskResponse;
import com.zsh.zshpicturebackend.api.imagesearch.ImageSearchResult;
import com.zsh.zshpicturebackend.api.imagesearch.SearchImageApiFacade;
import com.zsh.zshpicturebackend.common.BaseResponse;
import com.zsh.zshpicturebackend.model.dto.DeleteRequest;
import com.zsh.zshpicturebackend.common.ResultUtils;
import com.zsh.zshpicturebackend.constant.UserConstant;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.picture.*;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zsh.zshpicturebackend.model.vo.PictureTagCategory;
import com.zsh.zshpicturebackend.model.vo.PictureVO;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.service.SpaceService;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
public class PictureController {

    public static final List<String> TAG_LIST = Arrays.asList("热门", "搞笑", "生活", "高清", "校园", "创意");
    public static final List<String> CATEGORY_LIST = Arrays.asList("摄影", "艺术", "影视", "游戏", "动漫", "表情包");

    @Autowired
    private PictureService pictureService;
    @Autowired
    private UserService userService;
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private AIOutPaintingApi aiOutPaintingApi;
    @Autowired
    private AIText2ImageApi aiText2ImageApi;

    // 上传本地图片（可以重新上传：基础信息不变，只改变图片文件）
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadLocalPicture(@RequestPart("file") MultipartFile multipartFile,
                                                      PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    // 上传url图片（可以重新上传：基础信息不变，只改变图片文件）
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadUrlPicture(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
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
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
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
        pictureService.fillReviewParams(newPicture, loginUser);
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
        // 调用业务层的编辑图片方法
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(newPicture, loginUser);
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
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 用户不能查看别的用户私有空间的图片
        if (picture.getSpaceId() != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(picture, loginUser);
        }
        PictureVO pictureVO = pictureService.obj2vo(picture);
        return ResultUtils.success(pictureVO);
    }

    // 分页获取图片列表（仅管理员可用）
    //! 只展示公共图库的图片，不展示用户私有空间的图片
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureService.getPicturePage(pictureQueryRequest);
        return ResultUtils.success(picturePage);
    }

    // 分页获取PictureVO列表
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制爬虫，防止恶意用户一页展示所有数据然后全部爬走
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 100, ErrorCode.PARAMS_ERROR, "一页展示数据条数过多");

        Long spaceId = pictureQueryRequest.getSpaceId();
        Page<PictureVO> pictureVOPage;
        if (spaceId == null) {
            // 公共图库
            pictureQueryRequest.setNullSpaceId(true);
            // 普通用户默认只能查看已过审的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            // 分页获取PictureVO对象（有缓存）
            pictureVOPage = pictureService.getPictureVOPageWithCache(pictureQueryRequest);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该空间的管理员，无权查看该空间的图片");
            }
            Page<Picture> picturePage = pictureService.getPicturePage(pictureQueryRequest);
            pictureVOPage = pictureService.getPictureVOPage(picturePage);
        }

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
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean res = pictureService.reviewPicture(pictureReviewRequest, loginUser);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "审核失败");
        return ResultUtils.success(true);
    }

    // 批量抓取并上传url图片（仅管理员可用），返回成功上传的图片数
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadUrlPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadUrlPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    // 以图搜图
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchImageByImage(@RequestBody SearchImageByImageRequest request){
        ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        Long pictureId = request.getPictureId();
        ThrowUtils.throwIf(pictureId==null || pictureId<=0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> imageList = SearchImageApiFacade.searchImageByImage(picture.getUrl());
        return ResultUtils.success(imageList);
    }

    // 根据颜色搜索图片
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorRequest==null,ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    // 批量编辑图片
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
                                                    HttpServletRequest request){
        ThrowUtils.throwIf(pictureEditByBatchRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest,loginUser);
        return ResultUtils.success(true);
    }

    // 创建扩图任务
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(@RequestBody AIOutPaintingRequest aiOutPaintingRequest,
                                                                             HttpServletRequest request){
        ThrowUtils.throwIf(aiOutPaintingRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createOutPaintingTask(aiOutPaintingRequest, loginUser);
        return ResultUtils.success(response);
    }

    // 根据任务id查询扩图任务
    //! 仅给前端开发者调用
    @GetMapping("/out_painting/query_task")
    public BaseResponse<QueryOutPaintingTaskResponse> queryOutPaintingTaskByTaskId(String taskId){
        ThrowUtils.throwIf(StrUtil.isBlank(taskId),ErrorCode.PARAMS_ERROR);
        QueryOutPaintingTaskResponse response = aiOutPaintingApi.queryOutPaintingTaskByTaskId(taskId);
        return ResultUtils.success(response);
    }

    // 创建文生图任务
    @PostMapping("/text2image/create_task")
    public BaseResponse<ImageGenerationResult> createText2ImageTask(@RequestBody AIText2ImageRequest aiText2ImageRequest,
                                                                    HttpServletRequest request){
        ThrowUtils.throwIf(aiText2ImageRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser=userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        String text = aiText2ImageRequest.getText();
        AIText2ImageRequest.Parameters parameters = aiText2ImageRequest.getParameters();
        ThrowUtils.throwIf(StrUtil.isBlank(text) || parameters==null,ErrorCode.PARAMS_ERROR);
        ImageGenerationResult result = aiText2ImageApi.createText2ImageTask(aiText2ImageRequest);
        return ResultUtils.success(result);
    }

    // 查询文生图任务
    @GetMapping("/text2image/query_task")
    public BaseResponse<QueryText2ImageTaskResponse> queryText2ImageTaskByTaskId(String taskId){
        ThrowUtils.throwIf(StrUtil.isBlank(taskId),ErrorCode.PARAMS_ERROR);
        QueryText2ImageTaskResponse response = aiText2ImageApi.queryText2ImageTaskByTaskId(taskId);
        return ResultUtils.success(response);
    }

}
