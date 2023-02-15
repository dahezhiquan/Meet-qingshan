package com.qingshan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qingshan.dto.Result;
import com.qingshan.entity.Follow;
import com.qingshan.mapper.FollowMapper;
import com.qingshan.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * 关注取关接口实现类
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    /**
     * 关注或者取关操作
     *
     * @param followUserId 被关注过取关的用户id
     * @param isFollow     是关注操作还是取关操作
     * @return Result
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {

        Long userId = UserHolder.getUser().getId();

        // 判断是关注还是取关
        if (isFollow) {
            // 关注：新增记录到follow表
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        } else {
            // 取关：从follow表中删除数据
            remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
        }
        return Result.ok();
    }

    /**
     * 该用户是否被当前登录用户关注
     *
     * @param followUserId 被关注过取关的用户id
     * @return Result
     */
    @Override
    public Result isFollow(Long followUserId) {

        Long userId = UserHolder.getUser().getId();

        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();

        return Result.ok(count > 0);
    }
}
