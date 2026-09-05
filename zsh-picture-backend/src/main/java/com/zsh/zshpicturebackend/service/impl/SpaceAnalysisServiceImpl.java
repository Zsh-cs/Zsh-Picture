package com.zsh.zshpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static com.zsh.zshpicturebackend.constant.SizeConstant.*;

import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceRankAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceUserAnalyzeRequest;
import com.zsh.zshpicturebackend.model.dto.space.analysis.SpaceAnalyzeRequest;
import com.zsh.zshpicturebackend.model.entity.Picture;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.vo.space.analysis.*;
import com.zsh.zshpicturebackend.service.PictureService;
import com.zsh.zshpicturebackend.service.SpaceAnalysisService;
import com.zsh.zshpicturebackend.service.SpaceService;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalysisServiceImpl implements SpaceAnalysisService {

    @Autowired
    private UserService userService;
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private PictureService pictureService;

    // 校验空间分析权限
    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 分析全部空间/公共图库：仅管理员
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 分析某个私有空间：空间创建人或管理员
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(space, loginUser);
        }
    }

    // 根据空间分析请求填充queryWrapper
    @Override
    public void fillQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            // 管理员分析全部空间，不添加过滤条件
            return;
        }
        if (queryPublic) {
            // 管理员分析公共图库
            queryWrapper.isNull("spaceId");
            return;
        }
        if (spaceId != null) {
            // 空间创建人或管理员分析某个私有空间
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        // 其他情况抛异常
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
    }

    // 分析空间使用情况
    @Override
    public SpaceUsageVO analyzeSpaceUsage(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();

        // 分析全部空间/公共图库：仅管理员
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
            // 统计公共图库的资源使用情况：select picSize from picture where spaceId=null
            QueryWrapper<Picture> qw = new QueryWrapper<>();
            qw.select("picSize");
            qw.isNull(queryPublic, "spaceId");
            //? 使用mapper的selectObjs直接返回需要的列，节省存储空间，提高性能
            List<Object> picSizeList = pictureService.getBaseMapper().selectObjs(qw);
            long usedCount = picSizeList.size();
            long usedSize = 0;
            for (Object picSize : picSizeList) {
                if (picSize != null) {
                    usedSize += (Long) picSize;
                }
            }
            // 封装返回结果
            return SpaceUsageVO.builder()
                    .usedSize(usedSize).maxSize(null).sizeUsageRatio(null)
                    .usedCount(usedCount).maxCount(null).countUsageRatio(null)
                    .build();
        } else {
            // 分析某个私有空间：空间创建人或管理员
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(space, loginUser);
            // 封装返回结果
            return SpaceUsageVO.builder()
                    .usedSize(space.getTotalSize())
                    .maxSize(space.getMaxSize())
                    .sizeUsageRatio(NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue())
                    .usedCount(space.getTotalCount())
                    .maxCount(space.getMaxCount())
                    .countUsageRatio(NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue())
                    .build();
        }
    }

    // 空间图片分类分析
    @Override
    public List<SpaceCategoryAnalyzeVO> analyzeSpaceByPictureCategory(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 校验空间分析权限
        checkSpaceAnalyzeAuth(spaceAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillQueryWrapper(spaceAnalyzeRequest, qw);
        // select category, count(*) as count, sum(picSize) as totalSize from picture group by category
        qw.select("category", "count(*) as count", "sum(picSize) as totalSize").groupBy("category");
        // 查询并封装返回结果
        List<Map<String, Object>> maps = pictureService.getBaseMapper().selectMaps(qw);
        return maps.stream()
                .map(m->SpaceCategoryAnalyzeVO.builder()
                        .category(m.get("category") != null ? m.get("category").toString() : "未分类")
                        .count(((Number) m.get("count")).longValue())
                        .totalSize(((Number) m.get("totalSize")).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    // 空间图片标签分析
    @Override
    public List<SpaceTagAnalyzeVO> analyzeSpaceByPictureTag(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 校验空间分析权限
        checkSpaceAnalyzeAuth(spaceAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillQueryWrapper(spaceAnalyzeRequest, qw);
        qw.select("tags").isNotNull("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(qw)
                .stream().map(Object::toString).collect(Collectors.toList());
        // 统计每个标签的使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 封装返回结果，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(e -> new SpaceTagAnalyzeVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // 空间图片大小分析
    @Override
    public List<SpaceSizeAnalyzeVO> analyzeSpaceByPictureSize(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 校验空间分析权限
        checkSpaceAnalyzeAuth(spaceAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillQueryWrapper(spaceAnalyzeRequest, qw);
        qw.select("picSize");
        // 查询并封装返回结果
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(qw).stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());
        long count1 = 0L, count2 = 0L, count3 = 0L, count4 = 0L;
        for (Long picSize : picSizeList) {
            if (picSize < 100 * ONE_KB) {
                count1++;
            } else if (picSize < 500 * ONE_KB) {
                count2++;
            } else if (picSize < ONE_MB) {
                count3++;
            } else {
                count4++;
            }
        }
        return List.of(
                new SpaceSizeAnalyzeVO("<100KB", count1),
                new SpaceSizeAnalyzeVO("100KB~500KB", count2),
                new SpaceSizeAnalyzeVO("500KB~1MB", count3),
                new SpaceSizeAnalyzeVO(">=1MB", count4)
        );
    }

    // 空间用户上传行为分析
    @Override
    public List<SpaceUserAnalyzeVO> analyzeSpaceUser(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        Long userId = spaceUserAnalyzeRequest.getUserId();
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        // 校验空间分析权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        fillQueryWrapper(spaceUserAnalyzeRequest, qw);
        qw.eq(userId!=null,"userId",userId);
        switch (timeDimension){
            case "day":
                qw.select("date_format(createTime,'%Y-%m-%d') as period","count(*) as count");
                break;
            case "week":
                qw.select("date_format(createTime,'%x年%v周') as period","count(*) as count");
                break;
            case "month":
                qw.select("date_format(createTime,'%Y-%m') as period","count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持该时间维度");
        }
        qw.groupBy("period").orderByAsc("period");
        // 查询并封装返回结果
        List<Map<String, Object>> maps = pictureService.getBaseMapper().selectMaps(qw);
        return maps.stream()
                .map(m->SpaceUserAnalyzeVO.builder()
                        .period(m.get("period").toString())
                        .count(((Number) m.get("count")).longValue())
                        .build())
                .collect(Collectors.toList());

    }

    // 空间使用排行分析：仅管理员
    @Override
    public List<Space> analyzeSpaceRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest){
        Integer topN = spaceRankAnalyzeRequest.getTopN();
        LambdaQueryWrapper<Space> lqw=new LambdaQueryWrapper<>();
        lqw.orderByDesc(Space::getTotalSize)
                .last("limit "+topN);
        return spaceService.list(lqw);
    }
}
