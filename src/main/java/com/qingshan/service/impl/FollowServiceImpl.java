package com.qingshan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qingshan.dto.Result;
import com.qingshan.dto.UserDTO;
import com.qingshan.entity.Follow;
import com.qingshan.entity.User;
import com.qingshan.mapper.FollowMapper;
import com.qingshan.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.service.IUserService;
import com.qingshan.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qingshan.utils.RedisConstants.FOLLOW_KEY;

/**
 * 关注取关接口实现类
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

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
            boolean isSave = save(follow);
            if (isSave) {
                // 把关注用户的id,放入redis的set集合中
                stringRedisTemplate.opsForSet().add(FOLLOW_KEY + userId, followUserId.toString());
            }
        } else {
            // 取关：从follow表中删除数据
            boolean isRemove = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (isRemove) {
                // 把关注用户的id从Redis集合中移除
                stringRedisTemplate.opsForSet().remove(FOLLOW_KEY + userId, followUserId.toString());
            }
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

    /**
     * 查询和某一个用户的共同关注，利用Redis set集合交集实现
     *
     * @param id 对方的id
     * @return Result
     */
    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String keyOfMine = FOLLOW_KEY + userId;
        String keyOfOther = FOLLOW_KEY + id;
        Set<String> commonFollows = stringRedisTemplate.opsForSet().intersect(keyOfMine, keyOfOther);
        if (commonFollows == null || commonFollows.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 解析id
        List<Long> ids = commonFollows.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOS = users.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
