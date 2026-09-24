package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
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

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = getBaseMapper().pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteBatch(List<Long> ids) {
        // 查询要删除的套餐
        List<Setmeal> setmeals = getBaseMapper().selectByIds(ids);
        // 判断是否起售中
        setmeals.forEach(setmeal -> {
            if (setmeal.getStatus().equals(StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        // 删除
        removeBatchByIds(ids);
        Db.lambdaUpdate(SetmealDish.class).in(SetmealDish::getSetmealId, ids).remove();
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = getById(id);
        List<SetmealDish> setmealDishes = Db.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, id).list();

        SetmealVO setmealVO = BeanUtil.copyProperties(setmeal, SetmealVO.class);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSetmeal(SetmealDTO setmealDTO) {
        // 转换成Setmeal
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        Long setmealId = setmealDTO.getId();

        // 修改套餐
        lambdaUpdate().eq(Setmeal::getId, setmeal.getId())
                // 字符串字段：name / description / image
                .set(setmeal.getName() != null && !setmeal.getName().isBlank(), Setmeal::getName, setmeal.getName())
                .set(setmeal.getDescription() != null && !setmeal.getDescription().isBlank(), Setmeal::getDescription, setmeal.getDescription())
                .set(setmeal.getImage() != null && !setmeal.getImage().isBlank(), Setmeal::getImage, setmeal.getImage())
                // 数值类型字段：categoryId、price、status，只判null
                .set(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .set(setmeal.getPrice() != null, Setmeal::getPrice, setmeal.getPrice())
                .set(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus())
                .update();

        // 删除套餐与菜品的关联
        Db.lambdaUpdate(SetmealDish.class).eq(SetmealDish::getSetmealId, setmealId).remove();
        // 重新插入菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        Db.saveBatch(setmealDishes);
    }
}
