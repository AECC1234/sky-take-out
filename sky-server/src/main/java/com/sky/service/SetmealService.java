package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);
}
