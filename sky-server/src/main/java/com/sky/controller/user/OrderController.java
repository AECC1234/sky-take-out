package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
//        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
//        log.info("生成预支付交易单：{}", orderPaymentVO);

//        return Result.success(orderPaymentVO);

        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());

        return Result.success();
    }

    @GetMapping("/historyOrders")
    @ApiOperation("用户历史订单分页查询")
    public Result<PageResult> page(Integer page, Integer pageSize, Integer status) {
        log.info("用户订单分页查询: {}页, 每页{}条, 状态{}", page, pageSize, status);
        OrdersPageQueryDTO pageQueryDTO = new OrdersPageQueryDTO();
        pageQueryDTO.setPage(page);
        pageQueryDTO.setPageSize(pageSize);
        pageQueryDTO.setStatus(status);

        return Result.success(orderService.pageQuery(pageQueryDTO));
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详细")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        log.info("查询订单明细: {}", id);
        return Result.success(orderService.details(id));
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }
}
