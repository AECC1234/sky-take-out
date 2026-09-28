package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        // 判断商品是否已经存在
        ShoppingCart cart = lambdaQuery()
                .eq(shoppingCart.getUserId() != null, ShoppingCart::getUserId, shoppingCart.getUserId())
                .eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId())
                .eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId())
                .eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor())
                .one();
        // 存在则+1
        if (cart != null) {
            cart.setNumber(cart.getNumber() + 1);
            lambdaUpdate().set(ShoppingCart::getNumber, cart.getNumber())
                    .eq(ShoppingCart::getUserId, cart.getUserId())
                    .update();
        } else {
            // 不存在则新增1条
            Long dishId = shoppingCartDTO.getDishId();
            Long setmealId = shoppingCartDTO.getSetmealId();

            // 判断是菜品还是套餐
            if (dishId != null && setmealId == null) {
                Dish dish = Db.getById(dishId, Dish.class);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else if (dishId == null && setmealId != null) {
                Setmeal setmeal = Db.getById(setmealId, Setmeal.class);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            shoppingCart.setNumber(1);

            // 插入数据
            getBaseMapper().insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        return lambdaQuery().eq(ShoppingCart::getUserId, BaseContext.getCurrentId()).list();
    }

    @Override
    public void cleanShoppingCart() {
        lambdaUpdate().eq(ShoppingCart::getUserId, BaseContext.getCurrentId()).remove();
    }
}
