package com.sky.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService extends IService<Orders> {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 分页查询历史订单
     * @param pageQueryDTO
     * @return
     */
    PageResult pageQuery(OrdersPageQueryDTO pageQueryDTO);

    /**
     * 查询订单明细
     * @param id
     * @return
     */
    OrderVO details(Integer id);

    /**
     * 取消订单
     * @param id
     */
    void cancel(Integer id);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Integer id);

    /**
     * 订单的分页条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
}
