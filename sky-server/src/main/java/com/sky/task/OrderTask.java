package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOutOrder() {
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        LambdaQueryWrapper<Orders> wrapper = new QueryWrapper<Orders>().lambda()
                .eq(Orders::getStatus, Orders.UN_PAID)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusMinutes(15));

        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) return;

        List<Long> idLists = orders.stream().map(Orders::getId).toList();

        LambdaUpdateWrapper<Orders> updateWrapper = new UpdateWrapper<Orders>().lambda()
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, "订单超时")
                .set(Orders::getCancelTime, LocalDateTime.now())
                .in(Orders::getId, idLists);

        orderMapper.update(updateWrapper);
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processDeliveryOrder() {
        log.info("定时处理派送中的订单: {}", LocalDateTime.now());

        LambdaQueryWrapper<Orders> wrapper = new QueryWrapper<Orders>().lambda()
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, LocalDateTime.now().minusHours(1));

        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) return;

        List<Long> idLists = orders.stream().map(Orders::getId).toList();

        LambdaUpdateWrapper<Orders> updateWrapper = new UpdateWrapper<Orders>().lambda()
                .set(Orders::getStatus, Orders.COMPLETED)
                .in(Orders::getId, idLists);

        orderMapper.update(updateWrapper);
    }
}
