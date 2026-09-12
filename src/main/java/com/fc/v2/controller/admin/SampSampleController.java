package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampSample;
import com.fc.v2.service.ITSampLotService;
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

    @Autowired
    private ITSampLotService tSampLotService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        mmap.put("lotId", lotId);
        mmap.put("lots", tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()));
        if (lotId != null) {
            mmap.put("lot", tSampLotService.selectTSampLotById(lotId));
        }
        return prefix + "/list";
    }

    @Log(title = "样本检测记录集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampSample tSampSample) {
        QueryWrapper<TSampSample> queryWrapper = new QueryWrapper<TSampSample>();
        // 按检验批查这个批下面的样本，编号排序在service中统一处理
        queryWrapper.eq(tSampSample.getLotId() != null, "lot_id", tSampSample.getLotId());
        queryWrapper.like(StringUtils.isNotEmpty(tSampSample.getSampleNo()), "sample_no", tSampSample.getSampleNo());
        startPage();
        PageInfo<TSampSample> page = new PageInfo<TSampSample>(tSampSampleService.selectTSampSampleList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "新增跳转", notes = "新增跳转")
    @GetMapping("/add")
    public String add(@RequestParam Long lotId, ModelMap mmap) {
        TSampLot lot = tSampLotService.selectTSampLotById(lotId);
        java.util.List<String> recordedNos = lot == null
                ? java.util.Collections.<String>emptyList()
                : tSampSampleService.selectRecordedSampleNos(lotId);
        mmap.put("lotId", lotId);
        mmap.put("lot", lot);
        mmap.put("recordedCount", recordedNos.size());
        // 样本编号服务端生成，页面只做提示，提交时以后端实际生成结果为准
        String nextNo = null;
        if (lot != null && lot.getSampleSize() != null) {
            for (int i = 1; i <= lot.getSampleSize(); i++) {
                String no = String.format("S%02d", i);
                if (!recordedNos.contains(no)) {
                    nextNo = no;
                    break;
                }
            }
        }
        mmap.put("nextNo", nextNo);
        return prefix + "/add";
    }

    @Log(title = "样本检测记录新增", action = "add")
    @ApiOperation(value = "新增保存", notes = "新增保存")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(TSampSample tSampSample) {
        try {
            return toAjax(tSampSampleService.insertTSampSample(tSampSample));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "修改跳转", notes = "修改跳转")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        TSampSample sample = tSampSampleService.selectTSampSampleById(id);
        mmap.put("TSampSample", sample);
        if (sample != null) {
            mmap.put("lot", tSampLotService.selectTSampLotById(sample.getLotId()));
        }
        return prefix + "/edit";
    }

    @Log(title = "样本检测记录修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(TSampSample tSampSample) {
        try {
            return toAjax(tSampSampleService.updateTSampSample(tSampSample));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "样本检测记录删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        try {
            return toAjax(tSampSampleService.deleteTSampSampleByIds(ids));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
