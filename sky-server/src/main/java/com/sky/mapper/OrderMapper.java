package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 求和
     * @param wrapper
     * @return
     */
    Double sum(@Param(Constants.WRAPPER) LambdaQueryWrapper<Orders> wrapper);

    /**
     * 统计总数量
     * @param wrapper
     * @return
     */
    Integer count(@Param(Constants.WRAPPER) LambdaQueryWrapper<Orders> wrapper);

    /**
     * 修改订单信息
     * @param orders
     */
//    void update(Orders orders);
}
