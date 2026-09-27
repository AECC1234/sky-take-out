package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;

    /// 微信服务接口地址
    public static final String wxLogin = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());

        // 判断openid有效性
        if (openid == null || openid.isBlank()) {
            throw new LoginFailedException("登录失败");
        }

        // 是否为新用户
        User user = lambdaQuery().eq(User::getOpenid, openid).one();
        if (user == null) {
            // 注册
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            getBaseMapper().insert(user);
        }

        // 返回User对象
        return user;
    }

    private String getOpenId(String code) {
        // 调用微信服务器
        Map<String, String> params = new HashMap<>();
        params.put("appid", weChatProperties.getAppid());
        params.put("secret", weChatProperties.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String json = HttpClientUtil.doGet(wxLogin, params);
        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");
    }
}
