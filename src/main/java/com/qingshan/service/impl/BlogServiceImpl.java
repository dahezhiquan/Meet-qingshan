package com.qingshan.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingshan.dto.Result;
import com.qingshan.entity.Blog;
import com.qingshan.entity.User;
import com.qingshan.mapper.BlogMapper;
import com.qingshan.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.service.IUserService;
import com.qingshan.utils.SystemConstants;
import com.qingshan.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.qingshan.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * 博客服务实现类
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据博客查询用户信息，将用户信息封装到博客对象里传输
     *
     * @param blog 博客对象
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }


    /**
     * 根据ID查询博客
     *
     * @param id 博客ID
     * @return Result
     */
    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在啦！");
        }
        // 查询blog有关的用户信息
        queryBlogUser(blog);
        // 查询此blog是否被当前用户点赞了
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 判断当前blog是否被当前用户点赞了
     * 之后将注入是否点赞的属性到blog对象中
     *
     * @param blog 博客对象
     */
    private void isBlogLiked(Blog blog) {
        // 防止空指针异常
        if (UserHolder.getUser() == null) {
            return;
        }
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断用户是否已经点赞
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(BLOG_LIKED_KEY + blog.getId(), userId.toString());
        blog.setIsLike(BooleanUtil.isTrue(isLiked));
    }

    /**
     * 对指定id的博文进行点赞
     *
     * @param id 博文id
     * @return Result
     */
    @Override
    public Result likeBlog(Long id) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断用户是否已经点赞
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(BLOG_LIKED_KEY + id, userId.toString());

        if (BooleanUtil.isFalse(isLiked)) {
            // 未点赞，可以点赞
            // 数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到点赞redis集合中
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(BLOG_LIKED_KEY + id, userId.toString());
            }
        } else {
            // 已经点赞，取消点赞
            // 数据库点赞数 - 1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 将用户从点赞redis集合中移除
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(BLOG_LIKED_KEY + id, userId.toString());
            }
        }
        return Result.ok();
    }
}
