package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        TurnoverReportVO reportVO = new TurnoverReportVO();

        // 计算日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

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

            Double turnover = orderMapper.sumTurnover(wrapper);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        reportVO.setTurnoverList(StringUtils.join(turnoverList, ","));

        return reportVO;
    }
}
