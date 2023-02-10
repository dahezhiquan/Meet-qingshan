package com.qingshan.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.dto.LoginFormDTO;
import com.qingshan.dto.Result;
import com.qingshan.entity.User;
import com.qingshan.mapper.UserMapper;
import com.qingshan.service.IUserService;
import com.qingshan.utils.RegexUtils;
import com.qingshan.utils.SystemConstants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * 用户相关服务类接口实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送验证码
     *
     * @param phone   手机号
     * @param session 用户session信息
     * @return Result
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号码
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码
        session.setAttribute("code", code);

        // 发送验证码，调用第三方短信验证码API
        log.debug("发送短信验证码成功！" + code);
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录提交表单
     * @param session   用户session信息
     * @return Result
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        // 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 校验验证码，不一致，报错
        String cacheCode = (String) session.getAttribute("code");
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误！");
        }

        // 根据手机号查询用户
        User user = query().eq("phone", phone).one();

        // 判断用户是否存在
        if (user == null) {
            // 不存在，创建新用户
            user = createUserWithPhone(phone);
        }

        // 保证用户一定存在，保存用户信息到session中
        session.setAttribute("user", user);
        return Result.ok();
    }

    /**
     * 根据手机号创建新用户
     *
     * @param phone 用户手机号码
     * @return 一个用户对象
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        // 采用固定前缀 + 随机字符串的方式生成用户名
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
