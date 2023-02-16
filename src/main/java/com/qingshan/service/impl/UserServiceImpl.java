package com.qingshan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingshan.dto.LoginFormDTO;
import com.qingshan.dto.Result;
import com.qingshan.dto.UserDTO;
import com.qingshan.entity.User;
import com.qingshan.mapper.UserMapper;
import com.qingshan.service.IUserService;
import com.qingshan.utils.SystemConstants;
import com.qingshan.utils.UserHolder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.qingshan.utils.RedisConstants.*;
import static com.qingshan.utils.RegexUtils.isPhoneInvalid;

/**
 * 用户相关服务类接口实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


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
        if (isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码到Redis中，有效期为2分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

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
        if (isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        // Redis校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
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

        // 保存用户信息到Redis中
        // 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 将User对象转为Hash存储，此处要求userDTOMap的所有参数值均为String类型
        // 所以我们对BeanUtil.beanToMap做一个自定义
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName, filedValue) -> filedValue.toString()));
        // 存储数据到Redis中
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userDTOMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 返回token给客户端
        return Result.ok(token);
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

    /**
     * 签到
     *
     * @return Result
     */
    @Override
    public Result sign() {
        // 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        // 写入bitmap，进行签到
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    /**
     * 计算连续签到天数
     *
     * @return Result
     */
    @Override
    public Result signCount() {
        // 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();

        // 获取本月到今天的签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }

        Long num = result.get(0);

        if (num == null || num == 0) {
            return Result.ok(0);
        }

        // 连续签到天数
        int count = 0;

        // 循环lowbit运算
        while ((num & 1) != 0) {
            count++;
            num >>>= 1;
        }
        return Result.ok(count);
    }
}
