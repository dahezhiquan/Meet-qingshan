package com.qingshan.utils;

import com.qingshan.dto.UserDTO;
import com.qingshan.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器
 * 拦截用户的请求，校验session，进行用户校验
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session
        HttpSession session = request.getSession();
        // 获取session中的用户
        UserDTO user = (UserDTO) session.getAttribute("user");
        // 判断用户是否存在
        if (user == null) {
            // 用户不存在，拦截
            // 返回前端401状态码
            response.setStatus(401);
            return false;
        }
        // 存在，将session中的数据取出，保存用户信息到ThreadLocal
        UserHolder.saveUser(user);
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
