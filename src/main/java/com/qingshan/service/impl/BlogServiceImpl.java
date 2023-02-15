package com.qingshan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingshan.dto.Result;
import com.qingshan.dto.UserDTO;
import com.qingshan.entity.Blog;
import com.qingshan.entity.Follow;
import com.qingshan.entity.User;
import com.qingshan.mapper.BlogMapper;
import com.qingshan.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.service.IFollowService;
import com.qingshan.service.IUserService;
import com.qingshan.utils.SystemConstants;
import com.qingshan.utils.UserHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qingshan.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.qingshan.utils.RedisConstants.FEED_KEY;

/**
 * 博客服务实现类
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

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
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + blog.getId(), userId.toString());
        blog.setIsLike(score != null);
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
        // 判断用户是否已经点赞，看用户的点赞分数是否存在即可
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, userId.toString());
        if (score == null) {
            // 未点赞，可以点赞
            // 数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到点赞redis排序集合中，用当前的时间戳当作分数
            // 因为我们我按照点赞时间展示点赞列表
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(BLOG_LIKED_KEY + id, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 已经点赞，取消点赞
            // 数据库点赞数 - 1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 将用户从点赞redis排序集合中移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(BLOG_LIKED_KEY + id, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询博客的点赞列表
     *
     * @param id 博客id
     * @return Result
     */
    @Override
    public Result queryBlogLikes(Long id) {
        // 查询top的点赞用户
        Set<String> likedTop5 = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
        // 解析出用户id信息
        // 防止空指针异常
        if (likedTop5 == null || likedTop5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = likedTop5.stream().map(Long::valueOf).collect(Collectors.toList());
        // 将id拼接成字符串
        String idStr = StrUtil.join(",", ids);
        // 根据id查询用户
        List<User> users = userService.query().in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")").list();
        // 封装UserDTO对象
        List<UserDTO> userDTOs = BeanUtil.copyToList(users, UserDTO.class);
        // 返回用户信息
        return Result.ok(userDTOs);
    }

    /**
     * 保存博客
     *
     * @param blog 博客对象
     * @return 博客的ID
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSave = save(blog);
        if (!isSave) {
            return Result.fail("新增笔记失败！");
        }
        // 查询笔记作者的所有粉丝
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            // 开始推送
            stringRedisTemplate.opsForZSet().add(FEED_KEY + userId, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }
}
