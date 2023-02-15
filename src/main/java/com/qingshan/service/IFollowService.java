package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 关注取关服务接口类
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);
}
