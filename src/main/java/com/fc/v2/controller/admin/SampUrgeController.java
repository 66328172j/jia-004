package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampUrgeService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 检验批超期催办 controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Controller
@RequestMapping("/SampUrgeController")
@Api(value = "检验批超期催办")
public class SampUrgeController extends BaseController {

    private final String prefix = "admin/sampUrge";

    @Autowired
    private ITSampUrgeService tSampUrgeService;

    @Autowired
    private ITSampLotService tSampLotService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        mmap.put("lotId", lotId);
        mmap.put("lots", tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>()));
        return prefix + "/list";
    }

    @Log(title = "催办记录集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampUrge tSampUrge, @RequestParam(required = false) String onlyOverdue) {
        QueryWrapper<TSampUrge> queryWrapper = new QueryWrapper<TSampUrge>();
        // 按检验批查催办记录，按催办单号模糊查
        queryWrapper.eq(tSampUrge.getLotId() != null, "lot_id", tSampUrge.getLotId());
        queryWrapper.like(StringUtils.isNotEmpty(tSampUrge.getUrgeNo()), "urge_no", tSampUrge.getUrgeNo());
        // 只看还在超期的：检验批至今未判定且已超过要求完成天数
        if (StringUtils.isNotEmpty(onlyOverdue)) {
            List<Long> overdueLotIds = new ArrayList<Long>();
            for (TSampLot lot : tSampLotService.selectOverdueLotList()) {
                overdueLotIds.add(lot.getId());
            }
            if (overdueLotIds.isEmpty()) {
                return pageTable(new ArrayList<TSampUrge>(), 0);
            }
            queryWrapper.in("lot_id", overdueLotIds);
        }
        startPage();
        PageInfo<TSampUrge> page = new PageInfo<TSampUrge>(tSampUrgeService.selectTSampUrgeList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "发起跳转", notes = "发起跳转")
    @GetMapping("/add")
    public String add(@RequestParam(required = false) Long lotId, ModelMap mmap) {
        // 催办只针对当前还在超期的批，下拉只给这些批
        mmap.put("lots", tSampLotService.selectOverdueLotList());
        mmap.put("lotId", lotId);
        if (lotId != null) {
            TSampLot lot = tSampLotService.selectTSampLotById(lotId);
            mmap.put("lot", lot);
            mmap.put("overdueDays", tSampLotService.calcOverdueDays(lot));
        }
        return prefix + "/add";
    }

    @Log(title = "催办发起", action = "add")
    @ApiOperation(value = "发起保存", notes = "发起保存")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(TSampUrge tSampUrge) {
        try {
            return toAjax(tSampUrgeService.insertTSampUrge(tSampUrge));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "催办记录删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(tSampUrgeService.deleteTSampUrgeByIds(ids));
    }
}
