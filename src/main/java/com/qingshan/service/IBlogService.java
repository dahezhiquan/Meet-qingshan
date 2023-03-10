package com.qingshan.service;

import com.qingshan.dto.Result;
import com.qingshan.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 博客相关接口
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
