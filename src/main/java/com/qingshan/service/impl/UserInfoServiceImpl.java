package com.qingshan.service.impl;

import com.qingshan.entity.UserInfo;
import com.qingshan.mapper.UserInfoMapper;
import com.qingshan.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户详情页实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
