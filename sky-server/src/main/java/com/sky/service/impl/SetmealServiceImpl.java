package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        // 插入套餐
        save(setmeal);

        // 插入套餐和菜品的关系
        // 获取套餐ID
        Long setmealId = setmeal.getId();
        // 遍历赋值套餐ID
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));

        // 批量插入setmealDish
        Db.saveBatch(setmealDishes);
    }
}
