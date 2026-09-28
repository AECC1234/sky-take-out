package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
