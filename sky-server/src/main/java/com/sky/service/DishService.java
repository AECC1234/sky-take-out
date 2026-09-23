package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

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

    /**
     * 根据ID查询菜品对应的口味数据
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品基本信息和口味
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 根据分类ID查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> listByCategoryId(Long categoryId);
}
