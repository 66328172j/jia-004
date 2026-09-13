package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.service.ITSampProductService;
import com.fc.v2.service.ITSampStatisticsService;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.StringUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 质量统计 controller
 *
 * @author fuce
 * @date 2026-09-13
 */
@Controller
@RequestMapping("/SampStatisticsController")
@Api(value = "质量统计")
public class SampStatisticsController extends BaseController {

    private final String prefix = "admin/sampStatistics";

    @Autowired
    private ITSampStatisticsService tSampStatisticsService;

    @Autowired
    private ITSampProductService tSampProductService;

    @ApiOperation(value = "统计页跳转", notes = "统计页跳转")
    @GetMapping("/view")
    public String view(ModelMap mmap) {
        mmap.put("products", tSampProductService.selectTSampProductList(
                new QueryWrapper<TSampProduct>().eq("status", 0)));
        return prefix + "/list";
    }

    @Log(title = "质量统计按产品查询", action = "list")
    @ApiOperation(value = "按产品统计", notes = "按月份/产品过滤，返回各产品检验批次数、合格/不合格批数、合格率、一次检验通过率及汇总")
    @GetMapping("/byProduct")
    @ResponseBody
    public AjaxResult byProduct(@RequestParam(required = false) String beginMonth,
                                @RequestParam(required = false) String endMonth,
                                @RequestParam(required = false) Long productId) {
        try {
            Date beginTime = parseMonthBegin(beginMonth);
            Date endTime = parseMonthEnd(endMonth);
            validateRange(beginTime, endTime);
            return AjaxResult.successData(200,
                    tSampStatisticsService.statisticsByProduct(beginTime, endTime, productId));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "质量统计月度趋势查询", action = "list")
    @ApiOperation(value = "月度趋势", notes = "按月份/产品过滤，返回按月的检验批次数、合格/不合格批数、合格率、一次检验通过率")
    @GetMapping("/trendMonth")
    @ResponseBody
    public AjaxResult trendMonth(@RequestParam(required = false) String beginMonth,
                                 @RequestParam(required = false) String endMonth,
                                 @RequestParam(required = false) Long productId) {
        try {
            Date beginTime = parseMonthBegin(beginMonth);
            Date endTime = parseMonthEnd(endMonth);
            validateRange(beginTime, endTime);
            return AjaxResult.successData(200,
                    tSampStatisticsService.trendByMonth(beginTime, endTime, productId));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 页面传的月份为 yyyy-MM，作为开始时间取该月1号 00:00:00（含）
     */
    private Date parseMonthBegin(String month) {
        if (StringUtils.isEmpty(month)) {
            return null;
        }
        return parseMonth(month.trim());
    }

    /**
     * 结束月份按下月1号 00:00:00 作为排他边界，整月数据都算在内
     */
    private Date parseMonthEnd(String month) {
        if (StringUtils.isEmpty(month)) {
            return null;
        }
        Date begin = parseMonth(month.trim());
        Calendar c = Calendar.getInstance();
        c.setTime(begin);
        c.add(Calendar.MONTH, 1);
        return c.getTime();
    }

    private Date parseMonth(String month) {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.YYYY_MM);
        sdf.setLenient(false);
        try {
            return sdf.parse(month);
        } catch (ParseException e) {
            throw new IllegalArgumentException("月份格式不正确，应为 yyyy-MM");
        }
    }

    private void validateRange(Date beginTime, Date endTime) {
        if (beginTime != null && endTime != null && !beginTime.before(endTime)) {
            throw new IllegalArgumentException("开始月份不能晚于结束月份");
        }
    }
}
