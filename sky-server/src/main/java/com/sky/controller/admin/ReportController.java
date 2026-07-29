package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 统计报表管理
 */
@RestController
@RequestMapping("/admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {
    @Autowired
    private ReportService reportService;
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("营业额数据统计：{}到{}",begin,end);
        return Result.success(reportService.getTurnoverStatistics(begin,end));
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("用户数据统计：{}到{}",begin,end);
        return Result.success(reportService.getUserStatistics(begin,end));
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单数据统计：{}到{}",begin,end);
        return Result.success(reportService.getOrderStatistics(begin,end));
    }

    /**
     * 销量排名统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("销量排名统计")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("销量排名统计：{}到{}",begin,end);
        return Result.success(reportService.getSalesTop10(begin,end));
    }

    /**
     * 导出运营数据报表
     * @param begin 开始日期
     * @param end 结束日期
     * @param response HTTP响应
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            HttpServletResponse response) {
        // 参数为空时默认查询今天的数据
        if (begin == null) {
            begin = LocalDate.now();
        }
        if (end == null) {
            end = LocalDate.now();
        }
        log.info("导出运营数据报表：{}到{}", begin, end);
        reportService.exportBusinessData(begin, end, response);
    }

}
