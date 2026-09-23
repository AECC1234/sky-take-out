package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

	@Autowired
	private DishMapper dishMapper;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveDishAndFlavours(DishDTO dishDTO) {
		Dish dish = new Dish();
		dish = BeanUtil.copyProperties(dishDTO, Dish.class);
		getBaseMapper().insert(dish);

		Long dishId = dish.getId();

		List<DishFlavor> flavors = dishDTO.getFlavors();
		if (flavors != null && !flavors.isEmpty()) {
			flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
			Db.saveBatch(flavors);
		}
	}

	@Override
	public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
		PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
		Page<DishVO> dishVOPage = dishMapper.pageQuery(dishPageQueryDTO);
		return new PageResult(dishVOPage.getTotal(), dishVOPage.getResult());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteBatch(List<Long> ids) {
		// 判断当前菜品能否删除
		// 查询
		List<Dish> dishes = lambdaQuery().in(Dish::getId, ids).list();
		// 起售中
		dishes.forEach(dish -> {
			if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
				throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
			}
		});
		// 被套餐关联
		List<SetmealDish> setmealDishes = Db.lambdaQuery(SetmealDish.class).in(SetmealDish::getDishId, ids).list();
		List<Long> setmealIds = setmealDishes.stream().map(SetmealDish::getSetmealId).toList();

		if (!setmealIds.isEmpty()) {
			throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
		}
		// 删除菜品和对应的口味数据
		getBaseMapper().deleteByIds(ids);
		Db.lambdaUpdate(DishFlavor.class).in(DishFlavor::getDishId, ids).remove();
	}

	@Override
	public DishVO getByIdWithFlavor(Long id) {
		// 先查菜品表
		Dish dish = getById(id);
		// 再查口味表
		List<DishFlavor> dishFlavors = Db.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, id).list();
		// 封装
		DishVO dishVO = new DishVO();
		dishVO = BeanUtil.copyProperties(dish, DishVO.class);
		dishVO.setFlavors(dishFlavors);

		return dishVO;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateWithFlavor(DishDTO dishDTO) {
		// 修改菜品基本信息
		Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
		lambdaUpdate().eq(Dish::getId, dish.getId())
				.set(dish.getName() != null && !dish.getName().isBlank(), Dish::getName, dish.getName())
				.set(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId())
				.set(dish.getPrice() != null, Dish::getPrice, dish.getPrice())
				.set(dish.getImage() != null && !dish.getImage().isBlank(), Dish::getImage, dish.getImage())
				.set(dish.getDescription() != null && !dish.getDescription().isBlank(), Dish::getDescription, dish.getDescription())
				.set(dish.getStatus() != null, Dish::getStatus, dish.getStatus())
				.update();
		// 删除原有口味表
		Db.lambdaUpdate(DishFlavor.class).eq(DishFlavor::getDishId, dish.getId()).remove();
		// 判断是否有口味数据
		List<DishFlavor> flavors = dishDTO.getFlavors();
		if (flavors != null && !flavors.isEmpty()) {
			// 重新设置DishID
			flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
			// 重新插入口味数据
			Db.saveBatch(flavors);
		}
	}

	@Override
	public List<Dish> listByCategoryId(Long categoryId) {
        return lambdaQuery()
				.eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE)
				.orderByDesc(Dish::getCreateUser)
                .list();
	}
}