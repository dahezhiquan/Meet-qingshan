package com.qingshan.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.otp.TOTP;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qingshan.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.LOGIN_USER_KEY;
import static com.qingshan.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 登录拦截器
 * 拦截用户的请求，校验session，进行用户校验
 */
public class LoginInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("authorization");
        if (StringUtils.isBlank(token)) {
            // 用户不存在或未登录，拦截
            // 返回前端401状态码
            response.setStatus(401);
            return false;
        }
        // 基于token获取Redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        // 判断用户是否存在
        if (userMap.isEmpty()) {
            // 用户不存在或未登录，拦截
            // 返回前端401状态码
            response.setStatus(401);
            return false;
        }
        // 将Hash数据转为DTO数据，方便存储
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        // 刷新token的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 放行
        return true;
    }

    /**
     * 移除用户信息，防止ThreadLocal内存泄露
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
