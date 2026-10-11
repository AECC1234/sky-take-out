package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        TurnoverReportVO reportVO = new TurnoverReportVO();

        // 计算日期
        List<LocalDate> dateList = loopCalendar(begin, end);

        reportVO.setDateList(String.join(",", dateList.stream()
                        .map(date -> DateTimeFormatter.ofPattern("MM-dd").format(date)).toList()));

        // 查询营业额
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<Orders> wrapper = new QueryWrapper<Orders>().lambda()
                    .eq(Orders::getStatus, Orders.COMPLETED)
                    .between(Orders::getOrderTime, beginTime, endTime);

            Double turnover = orderMapper.sum(wrapper);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        reportVO.setTurnoverList(StringUtils.join(turnoverList, ","));

        return reportVO;
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        // 计算日期
        List<LocalDate> dateList = loopCalendar(begin, end);

        // 每天新增/总量
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>().lambda()
                    .le(User::getCreateTime, endTime);
            Integer totalUser = userMapper.count(wrapper);

            wrapper.ge(User::getCreateTime, beginTime);
            Integer newUser = userMapper.count(wrapper);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(String.join(",", dateList.stream()
                        .map(date -> DateTimeFormatter.ofPattern("MM-dd").format(date)).toList()))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 计算日期
        List<LocalDate> dateList = loopCalendar(begin, end);

        List<Integer> totalOrders = new ArrayList<>();
        List<Integer> validOrders = new ArrayList<>();

        // 遍历
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<Orders> wrapper = new QueryWrapper<Orders>().lambda();

            wrapper.between(Orders::getOrderTime, beginTime, endTime);
            totalOrders.add(orderMapper.count(wrapper));

            wrapper.eq(Orders::getStatus, Orders.COMPLETED);
            validOrders.add(orderMapper.count(wrapper));
        }

        // 订单总数
        Integer totalOrderCount = totalOrders.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrders.stream().reduce(Integer::sum).get();

        // 有效订单率
        double orderCompletionRate = 0.0;

        if (totalOrderCount != 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(String.join(",", dateList.stream()
                        .map(date -> DateTimeFormatter.ofPattern("MM-dd").format(date)).toList()))
                .orderCountList(StringUtils.join(totalOrders, ","))
                .validOrderCountList(StringUtils.join(validOrders, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @NotNull
    private static List<LocalDate> loopCalendar(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

    @Override
    public SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin, LocalDate end) {
        // 计算日期
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).toList();
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).toList();

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }
}
