package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public void exportBussinessData(HttpServletResponse response) {
        // 获取数据，查询数据库
        // 最近30天
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(dateEnd, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        // 写入execl文件
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
                XSSFWorkbook excel = new XSSFWorkbook(Objects.requireNonNull(is));
                OutputStream outputStream = response.getOutputStream()
        ) {
            // 填充概览数据
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间: " + dateBegin + " 至 " + dateEnd);
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            // 填充明细数据
            List<LocalDate> dateList = loopCalendar(dateBegin, dateEnd);
            int currentRowIndex = 7;
            for (LocalDate date : dateList) {
                beginTime = LocalDateTime.of(date, LocalTime.MIN);
                endTime = LocalDateTime.of(date, LocalTime.MAX);
                BusinessDataVO data = workspaceService.getBusinessData(beginTime, endTime);


                XSSFRow row = sheet.getRow(currentRowIndex);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());

                currentRowIndex++;
            }

            // 下载至浏览器
            excel.write(outputStream);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
