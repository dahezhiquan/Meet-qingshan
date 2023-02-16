package com.qingshan.service.impl;

import com.qingshan.entity.BlogComments;
import com.qingshan.mapper.BlogCommentsMapper;
import com.qingshan.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 博客评论实现类
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
