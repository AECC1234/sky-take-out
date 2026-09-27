package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

public interface UserService extends IService<User> {
    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
