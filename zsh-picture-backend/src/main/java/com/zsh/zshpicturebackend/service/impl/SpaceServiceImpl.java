package com.zsh.zshpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zsh.zshpicturebackend.exception.BusinessException;
import com.zsh.zshpicturebackend.exception.ErrorCode;
import com.zsh.zshpicturebackend.exception.ThrowUtils;
import com.zsh.zshpicturebackend.model.dto.space.SpaceAddRequest;
import com.zsh.zshpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zsh.zshpicturebackend.model.entity.Space;
import com.zsh.zshpicturebackend.model.entity.User;
import com.zsh.zshpicturebackend.model.enums.SpaceLevelEnum;
import com.zsh.zshpicturebackend.model.vo.SpaceVO;
import com.zsh.zshpicturebackend.model.vo.UserVO;
import com.zsh.zshpicturebackend.service.SpaceService;
import com.zsh.zshpicturebackend.mapper.SpaceMapper;
import com.zsh.zshpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author asus
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-08-13 00:22:29
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    // 存储锁对象
    private Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    // SpaceVO转Space
    @Override
    public Space vo2obj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }

    // Space转暂不包含UserVO的SpaceVO
    @Override
    public SpaceVO obj2incompleteVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }

    // Space转SpaceVO
    @Override
    public SpaceVO obj2vo(Space space) {
        SpaceVO spaceVO = this.obj2incompleteVO(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    // 校验空间
    @Override
    public void verifySpace(Space space) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        // 1.校验空间名称
        ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称为空");
        ThrowUtils.throwIf(spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称过长");
        // 2.校验空间级别
        ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别为空或不存在");
    }

    // 根据空间级别自动填充限额数据
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxCount = spaceLevelEnum.getMaxCount();
            long maxSize = spaceLevelEnum.getMaxSize();
            // 如果空间本身没有限额，才会自动填充，保证了灵活性
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
        }
    }

    // 将查询请求转化为QueryWrapper对象
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long id = request.getId();
        String spaceName = request.getSpaceName();
        Integer spaceLevel = request.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Long userId = request.getUserId();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        QueryWrapper<Space> qw = new QueryWrapper<>();
        qw.eq(id != null, "id", id)
                .like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName)
                .eq(spaceLevelEnum != null, "spaceLevel", spaceLevel)
                .eq(userId != null, "userId", userId)
                .orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return qw;

    }

    // 分页获取SpaceVO对象
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        List<SpaceVO> spaceVOList = spaceList.stream().map(this::obj2incompleteVO).collect(Collectors.toList());
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        List<User> userList = userService.listByIds(userIdSet);
        Map<Long, User> map = userList.stream().collect(Collectors.toMap(User::getId, user -> user));

        for (SpaceVO spaceVO : spaceVOList) {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (map.containsKey(userId)) {
                user = map.get(userId);
            }
            spaceVO.setUser(userService.getUserVO(user));
        }
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    // 创建空间
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1.将DTO转换为实体类，填充参数默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        this.fillSpaceBySpaceLevel(space);

        // 2.校验空间
        this.verifySpace(space);

        // 3.校验权限，非管理员只能创建普通版空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (!userService.isAdmin(loginUser) && !SpaceLevelEnum.COMMON.getValue().equals(space.getSpaceLevel())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员只能创建普通版空间");
        }

        // 4.控制同一用户只能创建一个私有空间
        //? 不同用户可以并行创建，所以不同用户获得的锁不一样；同一用户必须串行创建，所以同一用户获得的锁必须一样
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                // 添加编程式事务
                Long spaceId = transactionTemplate.execute(status -> {
                    // 判断该用户是否已经有了一个私有空间
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                    // 若已有空间，则不能创建
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有一个私有空间");
                    // 否则正常创建空间
                    boolean res = this.save(space);
                    ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                    return space.getId();
                });
                return Optional.ofNullable(spaceId).orElse(-1L);
            } finally {
                // 防止内存泄漏
                lockMap.remove(userId);
            }
        }
    }
}




