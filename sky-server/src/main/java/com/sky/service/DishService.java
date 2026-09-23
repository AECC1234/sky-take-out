package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;

/**
* @author 32085
* @description 针对表【dish(菜品)】的数据库操作Service
* @createDate 2026-09-23 03:44:07
*/
public interface DishService extends IService<Dish> {
    /**
     * 新增菜品
     * @param dishDTO
     */
    public void saveDishAndFlavours(DishDTO dishDTO);
}
