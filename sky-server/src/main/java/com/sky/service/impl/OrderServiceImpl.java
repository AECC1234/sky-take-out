package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理业务异常(地址簿为空, 购物车为空)
        AddressBook addressBook = Db.getById(ordersSubmitDTO.getAddressBookId(), AddressBook.class);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        List<ShoppingCart> carts = Db.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId())
                .list();
        if (carts == null || carts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 向订单表插入一条数据
        Orders orders = BeanUtil.copyProperties(ordersSubmitDTO, Orders.class);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());
        orders.setAddress(addressBook.getProvinceName()
                + addressBook.getCityName()
                + addressBook.getDistrictName()
                + addressBook.getDetail()
        );

        getBaseMapper().insert(orders);

        // 向明细表插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (ShoppingCart shoppingCart : carts) {
            OrderDetail orderDetail = BeanUtil.copyProperties(shoppingCart, OrderDetail.class);
            orderDetail.setOrderId(orders.getId());
            orderDetail.setId(null);
            orderDetails.add(orderDetail);
        }

        Db.saveBatch(orderDetails);

        // 下单成功清空购物车数据
        Db.lambdaUpdate(ShoppingCart.class).eq(ShoppingCart::getUserId, BaseContext.getCurrentId()).remove();

        // 封装VO
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = Db.getById(userId, User.class);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = getBaseMapper().getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        lambdaUpdate().eq(Orders::getId, ordersDB.getId())
                .set(ordersDB.getStatus() != null, Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .set(ordersDB.getPayStatus() != null, Orders::getPayStatus, Orders.PAID)
                .set(Orders::getCheckoutTime, LocalDateTime.now())
                .update();

        // 通过WebSocket向管理端发送消息
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public PageResult pageQuery(OrdersPageQueryDTO pageQueryDTO) {
        pageQueryDTO.setUserId(BaseContext.getCurrentId());

        // 构建分页查询条件
        Page<Orders> page = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());

        // 构建排序条件
        page.addOrder(OrderItem.desc("order_time"));

        LambdaQueryWrapper<Orders> wrapper = new QueryWrapper<Orders>().lambda()
              .eq(Orders::getUserId, pageQueryDTO.getUserId())
              .eq(pageQueryDTO.getStatus() != null, Orders::getStatus, pageQueryDTO.getStatus())
              .eq(pageQueryDTO.getPhone() != null, Orders::getPhone, pageQueryDTO.getPhone())
              .eq(pageQueryDTO.getNumber() != null, Orders::getNumber, pageQueryDTO.getNumber())
              .ge(pageQueryDTO.getBeginTime() != null, Orders::getOrderTime, pageQueryDTO.getBeginTime())
              .le(pageQueryDTO.getEndTime() != null, Orders::getOrderTime, pageQueryDTO.getEndTime());

        // 分页查询
        IPage<Orders> resultPage = getBaseMapper().selectPage(page, wrapper);
        long total = resultPage.getTotal();
        List<Orders> orders = resultPage.getRecords();

        List<OrderVO> orderVOS = BeanUtil.copyToList(orders, OrderVO.class);

        if (!resultPage.getRecords().isEmpty()) {
            // 循环查找所有订单明细
            for (OrderVO orderVO: orderVOS) {
                List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class)
                        .eq(OrderDetail::getOrderId, orderVO.getId())
                        .list();
                orderVO.setOrderDetailList(orderDetails);
            }
        }

        return new PageResult(total, orderVOS);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = getById(id);
        List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, id).list();
        OrderVO orderVO = BeanUtil.copyProperties(orders, OrderVO.class);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        Orders orders = getById(id);

        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 退款
        boolean needRefound = Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED);

        LambdaUpdateWrapper<Orders> wrapper = new UpdateWrapper<Orders>().lambda()
                .eq(Orders::getId, id)
                .set(needRefound, Orders::getPayStatus, Orders.REFUND)
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelTime, LocalDateTime.now())
                .set(Orders::getCancelReason, "用户取消订单");

        getBaseMapper().update(wrapper);
    }

    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, id).list();

        List<ShoppingCart> carts = BeanUtil.copyToList(orderDetails, ShoppingCart.class);
        carts.forEach(shoppingCart -> shoppingCart.setUserId(BaseContext.getCurrentId()));

        Db.saveBatch(carts);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        page.addOrder(OrderItem.desc("order_time"));

        List<Orders> orders = lambdaQuery()
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())
                .like(ordersPageQueryDTO.getPhone() != null, Orders::getPhone, ordersPageQueryDTO.getPhone())
                .like(ordersPageQueryDTO.getNumber() != null, Orders::getNumber, ordersPageQueryDTO.getNumber())
                .ge(ordersPageQueryDTO.getBeginTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .le(ordersPageQueryDTO.getEndTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .list(page);
        List<OrderVO> orderVOS = BeanUtil.copyToList(orders, OrderVO.class);

        for (OrderVO orderVO : orderVOS) {
            // 先查询订单对应的菜品
            List<OrderDetail> orderDetails = Db.lambdaQuery(OrderDetail.class)
                    .eq(OrderDetail::getOrderId, orderVO.getId())
                    .list();
            // 再拼接字符串
            StringBuilder sb = new StringBuilder();
            orderDetails.forEach(orderDetail -> sb.append(orderDetail.getName()).append("*")
                    .append(orderDetail.getNumber()).append(";"));
            // 最后再赋值
            orderVO.setOrderDishes(sb.toString());
        }

        return new PageResult(page.getTotal(), orderVOS);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO statisticsVO = new OrderStatisticsVO();
        statisticsVO.setDeliveryInProgress(lambdaQuery().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS).count().intValue());
        statisticsVO.setConfirmed(lambdaQuery().eq(Orders::getStatus, Orders.CONFIRMED).count().intValue());
        statisticsVO.setToBeConfirmed(lambdaQuery().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED).count().intValue());

        return statisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        lambdaUpdate().set(Orders::getStatus, Orders.CONFIRMED)
                .eq(Orders::getId, ordersConfirmDTO.getId())
                .update();
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = getById(ordersRejectionDTO.getId());

        if (!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
//
//        }

        lambdaUpdate().eq(Orders::getId, ordersRejectionDTO.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getRejectionReason, Objects.requireNonNull(ordersRejectionDTO.getRejectionReason()))
                .set(Orders::getCancelTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void delivery(Long id) {
        Orders orders = getById(id);

        if (!Objects.equals(orders.getStatus(), Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        lambdaUpdate().eq(Orders::getId, id)
                .set(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .set(Orders::getDeliveryTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void complete(Long id) {
        Orders orders = getById(id);

        if (!Objects.equals(orders.getStatus(), Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        lambdaUpdate().eq(Orders::getId, id)
                .set(Orders::getStatus, Orders.COMPLETED)
                .update();
    }
}
