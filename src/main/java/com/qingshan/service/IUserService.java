package com.qingshan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qingshan.dto.LoginFormDTO;
import com.qingshan.dto.Result;
import com.qingshan.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 用户相关服务类接口
 */
public interface IUserService extends IService<User> {
    // 发送验证码
    Result sendCode(String phone, HttpSession session);
    // 登录
    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
