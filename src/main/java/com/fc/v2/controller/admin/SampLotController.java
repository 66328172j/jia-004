package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import com.fc.v2.model.auto.TSampScheme;
import com.fc.v2.service.ITSampLotService;
import com.fc.v2.service.ITSampProductService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 检验批 controller
 *
 * @author fuce
 * @date 2026-09-11
 */
@Controller
@RequestMapping("/SampLotController")
@Api(value = "检验批")
public class SampLotController extends BaseController {

    private final String prefix = "admin/sampLot";

    @Autowired
    private ITSampLotService tSampLotService;

    @Autowired
    private ITSampProductService tSampProductService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view() {
        return prefix + "/list";
    }

    @Log(title = "检验批集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampLot tSampLot) {
        QueryWrapper<TSampLot> queryWrapper = new QueryWrapper<TSampLot>();
        queryWrapper.like(StringUtils.isNotEmpty(tSampLot.getLotNo()), "lot_no", tSampLot.getLotNo());
        startPage();
        PageInfo<TSampLot> page = new PageInfo<TSampLot>(tSampLotService.selectTSampLotList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "新增跳转", notes = "新增跳转")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("products", tSampProductService.selectTSampProductList(
                new QueryWrapper<TSampProduct>().eq("status", 0)));
        return prefix + "/add";
    }

    @Log(title = "检验批新增", action = "add")
    @ApiOperation(value = "新增保存", notes = "新增保存")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(TSampLot tSampLot) {
        return toAjax(tSampLotService.insertTSampLot(tSampLot));
    }

    @ApiOperation(value = "修改跳转", notes = "修改跳转")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("TSampLot", tSampLotService.selectTSampLotById(id));
        mmap.put("products", tSampProductService.selectTSampProductList(
                new QueryWrapper<TSampProduct>().eq("status", 0)));
        return prefix + "/edit";
    }

    @Log(title = "检验批修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TSampLot tSampLot) {
        TSampLot dbLot = tSampLotService.selectTSampLotById(tSampLot.getId());
        if (dbLot == null) {
            return error("检验批不存在或已删除");
        }
        // 已判定/已关闭的检验批批量锁定，防止批量与已确定的抽样方案不一致
        if (tSampLotService.isBatchQtyLocked(dbLot)
                && !Objects.equals(dbLot.getBatchQty(), tSampLot.getBatchQty())) {
            return error("该检验批已判定，批量不允许修改");
        }
        return toAjax(tSampLotService.updateTSampLot(tSampLot));
    }

    @ApiOperation(value = "按批量匹配抽样方案", notes = "按批量匹配抽样方案")
    @GetMapping("/matchScheme")
    @ResponseBody
    public AjaxResult matchScheme(Integer batchQty) {
        TSampScheme scheme = tSampLotService.matchScheme(batchQty);
        if (scheme == null) {
            return AjaxResult.error("批量[" + batchQty + "]未匹配到抽样方案");
        }
        return AjaxResult.successData(200, scheme);
    }

    @Log(title = "检验批删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(tSampLotService.deleteTSampLotByIds(ids));
    }
}
