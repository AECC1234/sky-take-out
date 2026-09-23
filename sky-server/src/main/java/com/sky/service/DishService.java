package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;

import java.util.List;

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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品的批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);
}
