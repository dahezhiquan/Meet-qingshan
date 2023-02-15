package com.qingshan.controller;


import com.qingshan.dto.Result;
import com.qingshan.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 关注和取关注
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注或者取关操作
     * @param followUserId 被关注过取关的用户id
     * @param isFollow 是关注操作还是取关操作
     * @return Result
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 该用户是否被当前登录用户关注
     * @param followUserId 被关注过取关的用户id
     * @return Result
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }
}
