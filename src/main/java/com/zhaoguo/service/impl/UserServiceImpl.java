package com.zhaoguo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaoguo.dto.LoginFormDTO;
import com.zhaoguo.dto.Result;
import com.zhaoguo.dto.UserDTO;
import com.zhaoguo.entity.User;
import com.zhaoguo.mapper.UserMapper;
import com.zhaoguo.service.IUserService;
import com.zhaoguo.utils.RegexUtils;
import com.zhaoguo.utils.SystemConstants;
import com.zhaoguo.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

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

    @Override
    public Result sendCode(String phone, HttpSession httpSession) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误，请输入正确的手机号！");
        }

        String code = RandomUtil.randomNumbers(6);

        httpSession.setAttribute("code", code);

        log.debug("发送手机验证码成功，发送的验证码为:{}", code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession httpSession) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误，请输入正确的手机号！");
        }

        if (httpSession.getAttribute("code") == null) {
            return Result.fail("请先获取验证码！");
        }

        if (!loginForm.getCode().equals(httpSession.getAttribute("code"))) {
            return Result.fail("登录失败，输入的验证码错误！");
        }

        User user = query().eq("phone", loginForm.getPhone()).one();

        if (user == null) {
            user = creatUserWithPhone(loginForm.getPhone());

        }

        httpSession.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        return Result.ok();
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