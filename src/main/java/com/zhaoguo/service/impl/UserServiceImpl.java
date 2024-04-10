package com.zhaoguo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaoguo.dto.LoginFormDTO;
import com.zhaoguo.dto.Result;
import com.zhaoguo.dto.UserDTO;
import com.zhaoguo.entity.User;
import com.zhaoguo.mapper.UserMapper;
import com.zhaoguo.service.IUserService;
import com.zhaoguo.utils.RedisConstants;
import com.zhaoguo.utils.RegexUtils;
import com.zhaoguo.utils.SystemConstants;
import com.zhaoguo.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ValueOperations<String, String> stringStringValueOperations;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误，请输入正确的手机号！");
        }

        String code = RandomUtil.randomNumbers(6);

        saveCodeInRedis(phone, code);

        log.debug("发送手机验证码成功，发送的验证码为:{}", code);

        return Result.ok();
    }

    private void saveCodeInRedis(String phone, String code) {
        stringStringValueOperations.set(RedisConstants.LOGIN_CODE_KEY + phone, code, Duration.ofMinutes(RedisConstants.LOGIN_CODE_TTL));
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误，请输入正确的手机号！");
        }

        if (!checkCode(loginForm.getPhone(), loginForm.getCode())) {
            return Result.fail("验证失败，请输入正确的验证码！");
        }

        User user = query().eq("phone", loginForm.getPhone()).one();

        if (user == null) {
            user = creatUserWithPhone(loginForm.getPhone());

        }

        String token = saveUserToRedis(user);

        return Result.ok(token);
    }

    private String saveUserToRedis(User user) {
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        HashOperations<String, Object, Object> stringObjectObjectHashOperations = stringRedisTemplate.opsForHash();
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
        );
        stringObjectObjectHashOperations.putAll(RedisConstants.LOGIN_USER_KEY + token, userDTOMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL));
        return token;
    }

    private boolean checkCode(String phone, String code) {
        String redisCode = stringStringValueOperations.get(RedisConstants.LOGIN_CODE_KEY + phone);
        return redisCode != null && redisCode.equals(code);
    }

    @Override
    public Result me(HttpSession httpSession) {
        return Result.ok(UserHolder.getUser());
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        return user;
    }
}