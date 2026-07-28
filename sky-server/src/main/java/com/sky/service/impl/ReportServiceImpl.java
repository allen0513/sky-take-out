package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1、查询时间区间内所有订单的日期，以及日期对应的订单金额
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            // 日期加1
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询指定日期内订单数据
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Double turnover = orderMapper.sumByMap(beginTime, endTime, Orders.COMPLETED);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);

        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
}
