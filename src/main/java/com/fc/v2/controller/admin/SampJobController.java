package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.quartz.SampJobScheduler;
import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.model.auto.TSampJobLog;
import com.fc.v2.service.ITSampJobService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 检验超期扫描定时任务 controller
 *
 * @author fuce
 * @date 2026-09-13
 */
@Controller
@RequestMapping("/SampJobController")
@Api(value = "检验超期扫描定时任务")
public class SampJobController extends BaseController {

    private final String prefix = "admin/sampJob";

    @Autowired
    private ITSampJobService tSampJobService;

    @Autowired
    private SampJobScheduler sampJobScheduler;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    public String view(ModelMap mmap) {
        return prefix + "/list";
    }

    @Log(title = "定时任务集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @ResponseBody
    public ResultTable list(TSampJob tSampJob) {
        QueryWrapper<TSampJob> queryWrapper = new QueryWrapper<TSampJob>();
        queryWrapper.like(StringUtils.isNotEmpty(tSampJob.getJobName()), "job_name", tSampJob.getJobName());
        queryWrapper.eq(tSampJob.getStatus() != null, "status", tSampJob.getStatus());
        startPage();
        PageInfo<TSampJob> page = new PageInfo<TSampJob>(tSampJobService.selectTSampJobList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "定时任务启停", action = "changeStatus")
    @ApiOperation(value = "任务启停", notes = "任务启停")
    @PutMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody TSampJob job) {
        TSampJob dbJob = tSampJobService.selectTSampJobById(job.getId());
        if (dbJob == null) {
            return error("任务不存在");
        }
        dbJob.setStatus(job.getStatus());
        tSampJobService.updateTSampJob(dbJob);
        // 同步调度器：停用即暂停，启用即恢复，之后不再/继续自动跑
        if (job.getStatus() != null && job.getStatus() == 1) {
            sampJobScheduler.pauseJob(dbJob);
        } else {
            sampJobScheduler.resumeJob(dbJob);
        }
        return toAjax(1);
    }

    @Log(title = "定时任务手动执行一次", action = "run")
    @ApiOperation(value = "手动执行一次", notes = "手动执行一次")
    @GetMapping("/run/{id}")
    @ResponseBody
    public AjaxResult run(@PathVariable("id") String id) {
        TSampJobLog jobLog = tSampJobService.runOverdueScan(id);
        if (jobLog == null) {
            return error("任务不存在或已停用，未执行");
        }
        return success("执行完成：扫描" + jobLog.getScanCount() + "批，生成催办"
                + jobLog.getUrgeCount() + "条，失败" + jobLog.getFailCount() + "条");
    }

    @ApiOperation(value = "任务日志跳转", notes = "任务日志跳转")
    @GetMapping("/logView")
    public String logView(@RequestParam(required = false) String jobName, ModelMap mmap) {
        mmap.put("jobName", jobName);
        return prefix + "/log";
    }

    @Log(title = "定时任务日志集合查询", action = "logList")
    @ApiOperation(value = "任务日志分页查询", notes = "任务日志分页查询")
    @GetMapping("/logList")
    @ResponseBody
    public ResultTable logList(TSampJobLog tSampJobLog) {
        QueryWrapper<TSampJobLog> queryWrapper = new QueryWrapper<TSampJobLog>();
        queryWrapper.like(StringUtils.isNotEmpty(tSampJobLog.getJobName()), "job_name", tSampJobLog.getJobName());
        queryWrapper.eq(tSampJobLog.getStatus() != null, "status", tSampJobLog.getStatus());
        startPage();
        PageInfo<TSampJobLog> page = new PageInfo<TSampJobLog>(tSampJobService.selectTSampJobLogList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }
}
