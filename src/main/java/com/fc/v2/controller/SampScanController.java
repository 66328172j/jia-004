package com.fc.v2.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampQueryLog;
import com.fc.v2.service.ITSampScanService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 移动端扫码查询 controller
 *
 * 客户在移动端扫码/手输批号查询检验结论：
 * /view、/query 在 ShiroFilterMapFactory 中配置为 anon，客户无需登录即可访问，
 * 且 /query 只通过 TSampScanVo 返回脱敏字段；
 * /logView、/logList 为内部查询记录管理，走默认登录认证。
 *
 * @author fuce
 * @date 2026-09-13
 */
@Controller
@RequestMapping("/SampScanController")
@Api(value = "移动端扫码查询")
public class SampScanController extends BaseController {

    private final String prefix = "admin/sampQueryLog";

    @Autowired
    private ITSampScanService tSampScanService;

    /**
     * 移动端查询页（客户扫码/手输批号），匿名访问
     */
    @ApiOperation(value = "扫码查询页", notes = "移动端扫码查询页")
    @GetMapping("/view")
    public String view() {
        return "mobile/sampScan";
    }

    /**
     * 按批号查询（客户扫码/手输），匿名访问。
     * 批号不存在时返回 hit=false 和明确提示，不抛异常、不返回 500。
     */
    @ApiOperation(value = "按批号查询", notes = "按批号查询检验状态、判定结论、质量等级（脱敏）")
    @GetMapping("/query")
    @ResponseBody
    public AjaxResult query(@RequestParam(required = false) String lotNo,
                            @RequestParam(required = false) String source) {
        Map<String, Object> result = tSampScanService.scanQuery(lotNo, source);
        Boolean hit = (Boolean) result.get("hit");
        // 未命中也返回 200，由页面按 hit/message 给客户明确提示，不算系统错误
        return AjaxResult.success(200, (String) result.get("message"), result.get("data"))
                .put("hit", Boolean.TRUE.equals(hit) ? 1 : 0);
    }

    /**
     * 查询记录管理页跳转（内部使用，需登录）
     */
    @ApiOperation(value = "查询记录分页跳转", notes = "查询记录分页跳转")
    @GetMapping("/logView")
    public String logView() {
        return prefix + "/list";
    }

    /**
     * 查询记录分页：批号、来源、命中标志、查询时间范围
     */
    @Log(title = "扫码查询记录集合查询", action = "list")
    @ApiOperation(value = "查询记录集合", notes = "查询记录集合")
    @GetMapping("/logList")
    @ResponseBody
    public ResultTable logList(TSampQueryLog tSampQueryLog,
                               @RequestParam(required = false) String beginTime,
                               @RequestParam(required = false) String endTime) {
        QueryWrapper<TSampQueryLog> queryWrapper = new QueryWrapper<TSampQueryLog>();
        queryWrapper.like(StringUtils.isNotEmpty(tSampQueryLog.getLotNo()),
                "lot_no", tSampQueryLog.getLotNo());
        queryWrapper.eq(StringUtils.isNotEmpty(tSampQueryLog.getQuerySource()),
                "query_source", tSampQueryLog.getQuerySource());
        queryWrapper.eq(tSampQueryLog.getResultFlag() != null,
                "result_flag", tSampQueryLog.getResultFlag());
        queryWrapper.ge(StringUtils.isNotEmpty(beginTime), "query_time", beginTime + " 00:00:00");
        queryWrapper.le(StringUtils.isNotEmpty(endTime), "query_time", endTime + " 23:59:59");
        startPage();
        PageInfo<TSampQueryLog> page = new PageInfo<TSampQueryLog>(
                tSampScanService.selectQueryLogList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    /**
     * 删除查询记录（内部使用）
     */
    @Log(title = "扫码查询记录删除", action = "remove")
    @ApiOperation(value = "查询记录删除", notes = "查询记录删除")
    @DeleteMapping("/logRemove")
    @ResponseBody
    public AjaxResult logRemove(String ids) {
        return toAjax(tSampScanService.deleteQueryLogByIds(ids));
    }
}
