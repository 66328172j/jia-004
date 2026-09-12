package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampRetestService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 检验批复检记录 controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Controller
@RequestMapping("/SampRetestController")
@Api(value = "检验批复检记录")
public class SampRetestController extends BaseController {

    private final String prefix = "admin/sampRetest";

    @Autowired
    private ITSampRetestService tSampRetestService;

    @Autowired
    private ITSampLotService tSampLotService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        mmap.put("lotId", lotId);
        mmap.put("lots", tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()));
        return prefix + "/list";
    }

    @Log(title = "复检单集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampRetest tSampRetest) {
        QueryWrapper<TSampRetest> queryWrapper = new QueryWrapper<TSampRetest>();
        // 按检验批复检单，按复检单号模糊查
        queryWrapper.eq(tSampRetest.getLotId() != null, "lot_id", tSampRetest.getLotId());
        queryWrapper.like(StringUtils.isNotEmpty(tSampRetest.getRetestNo()), "retest_no", tSampRetest.getRetestNo());
        queryWrapper.eq(tSampRetest.getStatus() != null, "status", tSampRetest.getStatus());
        startPage();
        PageInfo<TSampRetest> page = new PageInfo<TSampRetest>(tSampRetestService.selectTSampRetestList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "发起跳转", notes = "发起跳转")
    @GetMapping("/add")
    public String add(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        // 复检只针对判定不合格(3)的批，下拉只给这些批
        mmap.put("lots", tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()
                .eq("status", 3)
                .eq("conclude", "不合格")));
        mmap.put("lotId", lotId);
        if (lotId != null) {
            mmap.put("lot", tSampLotService.selectTSampLotById(lotId));
        }
        return prefix + "/add";
    }

    @Log(title = "复检单发起", action = "add")
    @ApiOperation(value = "发起保存", notes = "发起保存")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(TSampRetest tSampRetest) {
        try {
            return toAjax(tSampRetestService.insertTSampRetest(tSampRetest));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "录入结果跳转", notes = "录入结果跳转")
    @GetMapping("/finish/{id}")
    public String finish(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("TSampRetest", tSampRetestService.selectTSampRetestById(id));
        return prefix + "/finish";
    }

    @Log(title = "复检结果录入", action = "finish")
    @ApiOperation(value = "录入复检结果", notes = "录入复检结果")
    @PostMapping("/finish")
    @ResponseBody
    public AjaxResult finish(Long id, Integer retestResult) {
        try {
            return toAjax(tSampRetestService.finishTSampRetest(id, retestResult));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "复检单作废", action = "cancel")
    @ApiOperation(value = "作废复检单", notes = "作废复检单")
    @PostMapping("/cancel")
    @ResponseBody
    public AjaxResult cancel(Long id) {
        try {
            return toAjax(tSampRetestService.cancelTSampRetest(id));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "复检单删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(tSampRetestService.deleteTSampRetestByIds(ids));
    }
}
