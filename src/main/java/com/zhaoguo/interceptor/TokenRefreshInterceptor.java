package com.zhaoguo.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.zhaoguo.dto.UserDTO;
import com.zhaoguo.utils.RedisConstants;
import com.zhaoguo.utils.UserHolder;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

public class TokenRefreshInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public TokenRefreshInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (token.isEmpty()) {
            return true;
        }
        HashOperations<String, Object, Object> stringObjectObjectHashOperations = stringRedisTemplate.opsForHash();
        Map<Object, Object> map = stringObjectObjectHashOperations.entries(RedisConstants.LOGIN_USER_KEY + token);
        if (map.isEmpty()) {
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL));
        return true;
    }
}
