package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.service.ITSampSampleService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 检验样本检测记录 controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Controller
@RequestMapping("/SampSampleController")
@Api(value = "检验样本检测记录")
public class SampSampleController extends BaseController {

    private final String prefix = "admin/sampSample";

    @Autowired
    private ITSampSampleService tSampSampleService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view() {
        return prefix + "/list";
    }

    @Log(title = "样本检测记录集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampSample tSampSample) {
        QueryWrapper<TSampSample> queryWrapper = new QueryWrapper<TSampSample>();
        queryWrapper.eq(tSampSample.getLotId() != null, "lot_id", tSampSample.getLotId());
        queryWrapper.like(StringUtils.isNotEmpty(tSampSample.getSampleNo()), "sample_no", tSampSample.getSampleNo());
        startPage();
        PageInfo<TSampSample> page = new PageInfo<TSampSample>(tSampSampleService.selectTSampSampleList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "新增跳转", notes = "新增跳转")
    @GetMapping("/add")
    public String add(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        mmap.put("lotId", lotId);
        return prefix + "/add";
    }

    @Log(title = "样本检测记录新增", action = "add")
    @ApiOperation(value = "新增保存", notes = "新增保存")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(TSampSample tSampSample) {
        return toAjax(tSampSampleService.insertTSampSample(tSampSample));
    }

    @Log(title = "样本检测记录修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(TSampSample tSampSample) {
        return toAjax(tSampSampleService.updateTSampSample(tSampSample));
    }

    @Log(title = "样本检测记录删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(tSampSampleService.deleteTSampSampleByIds(ids));
    }
}
