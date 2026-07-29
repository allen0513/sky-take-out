package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
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

    /**
     * 统计指定时间区间内的用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        // 1、生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2、查询每天的新增用户数和总用户数
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 新增用户数量（当天注册的用户）
            Integer newUser = userMapper.countByMap(beginTime, endTime);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);

            // 总用户数量（截止到当天的所有用户）
            Integer totalUser = userMapper.countByMap(null, endTime);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);
        }

        // 3、封装并返回VO
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        // 1、生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2、查询每天的总订单数和有效订单数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 总订单数（当天创建的订单）
            Integer orderCount = orderMapper.countByMap(beginTime, endTime, null);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);

            // 有效订单数（当天完成的状态为COMPLETED的订单）
            Integer validOrderCount = orderMapper.countByMap(beginTime, endTime, Orders.COMPLETED);
            validOrderCount = validOrderCount == null ? 0 : validOrderCount;
            validOrderCountList.add(validOrderCount);
        }

        // 3、计算汇总数据
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
        }

        // 4、封装并返回VO
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        // 1、查询时间范围
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 2、查询销量排名top10
        List<Map<String, Object>> salesTop10 = orderDetailMapper.getSalesTop10(beginTime, endTime);

        // 3、提取名称和销量列表
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        for (Map<String, Object> item : salesTop10) {
            nameList.add((String) item.get("name"));
            // 数据库返回的total是BigDecimal或Long类型，转Integer
            Number total = (Number) item.get("total");
            numberList.add(total != null ? total.intValue() : 0);
        }

        // 4、封装并返回VO
        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     *
     * @param begin    开始日期
     * @param end      结束日期
     * @param response HTTP响应对象
     */
    @Override
    public void exportBusinessData(LocalDate begin, LocalDate end, HttpServletResponse response) {
        // 校验参数
        if (begin == null || end == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }

        // 1、生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2、查询每天的业务数据
        List<BusinessDataVO> businessDataList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询总订单数
            Integer totalOrderCount = orderMapper.countByMap(beginTime, endTime, null);

            // 营业额（已完成订单）
            Double turnover = orderMapper.sumByMap(beginTime, endTime, Orders.COMPLETED);
            turnover = turnover == null ? 0.0 : turnover;

            // 有效订单数（已完成订单）
            Integer validOrderCount = orderMapper.countByMap(beginTime, endTime, Orders.COMPLETED);

            Double unitPrice = 0.0;
            Double orderCompletionRate = 0.0;
            if (totalOrderCount != 0 && validOrderCount != 0) {
                // 订单完成率
                orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
                // 平均客单价
                unitPrice = turnover / validOrderCount;
            }

            // 新增用户数
            Integer newUsers = userMapper.countByMap(beginTime, endTime);

            businessDataList.add(BusinessDataVO.builder()
                    .turnover(turnover)
                    .validOrderCount(validOrderCount)
                    .orderCompletionRate(orderCompletionRate)
                    .unitPrice(unitPrice)
                    .newUsers(newUsers)
                    .build());
        }

        // 3、计算合计数据
        double sumTurnover = 0.0;
        int sumValidOrderCount = 0;
        int sumTotalOrderCount = 0;
        int sumNewUsers = 0;

        for (BusinessDataVO data : businessDataList) {
            sumTurnover += data.getTurnover();
            sumValidOrderCount += data.getValidOrderCount();
            sumNewUsers += data.getNewUsers();
        }
        // 总订单数（重新查询整个范围）
        sumTotalOrderCount = orderMapper.countByMap(
                LocalDateTime.of(dateList.get(0), LocalTime.MIN),
                LocalDateTime.of(dateList.get(dateList.size() - 1), LocalTime.MAX),
                null);

        // 合计订单完成率
        double sumOrderCompletionRate = 0.0;
        if (sumTotalOrderCount != 0 && sumValidOrderCount != 0) {
            sumOrderCompletionRate = (double) sumValidOrderCount / sumTotalOrderCount;
        }

        // 合计平均客单价
        double sumUnitPrice = 0.0;
        if (sumValidOrderCount != 0) {
            sumUnitPrice = sumTurnover / sumValidOrderCount;
        }

        // 4、加载模板并填充数据
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx")) {
            if (inputStream == null) {
                throw new RuntimeException("模板文件不存在：template/运营数据报表模板.xlsx");
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
                XSSFSheet sheet = workbook.getSheetAt(0);

                // 4.1 填充日期范围（模板 Row 2, B2:G2 合并）
                // POI 索引：Row 2 = index 1, Column B = index 1
                sheet.getRow(1).getCell(1).setCellValue(
                        "时间范围：" + dateList.get(0) + " ~ " + dateList.get(dateList.size() - 1));

                // 4.2 填充合计数据（模板 Row 4-5）
                // 模板 Row 4 (POI index 3): B4="营业额"(标签), C4=营业额值, D4="订单完成率"(标签), E4=订单完成率值, F4="新增用户数"(标签), G4=新增用户数值
                sheet.getRow(3).getCell(2).setCellValue(sumTurnover);         // C4 - 营业额
                sheet.getRow(3).getCell(4).setCellValue(sumOrderCompletionRate); // E4 - 订单完成率
                sheet.getRow(3).getCell(6).setCellValue(sumNewUsers);         // G4 - 新增用户数

                // 模板 Row 5 (POI index 4): B5="有效订单"(标签), C5=有效订单值, D5="平均客单价"(标签), E5=平均客单价值
                sheet.getRow(4).getCell(2).setCellValue(sumValidOrderCount);  // C5 - 有效订单数
                sheet.getRow(4).getCell(4).setCellValue(sumUnitPrice);        // E5 - 平均客单价

                // 4.3 填充每日明细数据（模板 Row 8-37, POI index 7-36）
                for (int i = 0; i < dateList.size(); i++) {
                    int rowIndex = 7 + i; // POI row index: 模板 Row 8 = index 7
                    if (rowIndex > 36) {
                        // 超过模板预留的30行数据区，复制样式创建新行
                        XSSFRow templateRow = sheet.getRow(36);
                        XSSFRow newRow = sheet.createRow(rowIndex);
                        newRow.setHeight(templateRow.getHeight());
                        for (int c = 0; c <= 6; c++) {
                            XSSFCell templateCell = templateRow.getCell(c);
                            XSSFCell newCell = newRow.createCell(c);
                            if (templateCell != null) {
                                newCell.setCellStyle(templateCell.getCellStyle());
                            }
                        }
                    }

                    XSSFRow row = sheet.getRow(rowIndex);
                    BusinessDataVO data = businessDataList.get(i);

                    // B (col 1): 日期
                    row.getCell(1).setCellValue(dateList.get(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    // C (col 2): 营业额
                    row.getCell(2).setCellValue(data.getTurnover());
                    // D (col 3): 有效订单
                    row.getCell(3).setCellValue(data.getValidOrderCount());
                    // E (col 4): 订单完成率
                    row.getCell(4).setCellValue(data.getOrderCompletionRate());
                    // F (col 5): 平均客单价
                    row.getCell(5).setCellValue(data.getUnitPrice());
                    // G (col 6): 新增用户
                    row.getCell(6).setCellValue(data.getNewUsers());
                }

                // 5、写出Excel到响应流
                String fileName = URLEncoder.encode("运营数据报表.xlsx", "UTF-8");
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

                workbook.write(response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (IOException e) {
            log.error("导出运营数据报表失败", e);
            throw new RuntimeException("导出运营数据报表失败", e);
        }
    }
}
