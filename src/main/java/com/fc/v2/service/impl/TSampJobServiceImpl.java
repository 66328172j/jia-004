package com.fc.v2.service.impl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.TSampJobLogMapper;
import com.fc.v2.mapper.auto.TSampJobMapper;
import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.model.auto.TSampJobLog;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.service.ITSampJobService;
import com.fc.v2.service.ITSampLotService;

/**
 * 定时任务Service实现（超期扫描）
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampJobServiceImpl extends ServiceImpl<TSampJobMapper, TSampJob> implements ITSampJobService {

    @Autowired
    private ITSampLotService tSampLotService;

    @Autowired
    private TSampUrgeMapper tSampUrgeMapper;

    @Autowired
    private TSampJobLogMapper tSampJobLogMapper;

    @Override
    public TSampJob selectTSampJobById(String id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<TSampJob> selectTSampJobList(Wrapper<TSampJob> queryWrapper) {
        QueryWrapper<TSampJob> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampJob>) queryWrapper
                : new QueryWrapper<TSampJob>();
        wrapper.eq("del_flag", 0).orderByAsc("create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<TSampJobLog> selectTSampJobLogList(Wrapper<TSampJobLog> queryWrapper) {
        QueryWrapper<TSampJobLog> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TSampJobLog>) queryWrapper
                : new QueryWrapper<TSampJobLog>();
        wrapper.orderByDesc("start_time");
        return tSampJobLogMapper.selectList(wrapper);
    }

    @Override
    public int updateTSampJob(TSampJob tSampJob) {
        return baseMapper.updateById(tSampJob);
    }

    /**
     * 执行一次超期扫描：扫描未判定且超期的检验批，逐批生成催办记录并落一条任务日志。
     * 单批生成失败只计入失败条数，不影响其他批；同一批同一天只自动催办一次，防止任务重复执行刷屏
     */
    @Override
    public TSampJobLog runOverdueScan(String jobId) {
        TSampJob job = baseMapper.selectById(jobId);
        // 任务不存在/已删除/已停用都不执行：停用即不再自动跑，页面手动触发同样拦截
        if (job == null || (job.getDelFlag() != null && job.getDelFlag() == 1)
                || (job.getStatus() != null && job.getStatus() == 1)) {
            return null;
        }

        TSampJobLog log = new TSampJobLog();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setJobName(job.getJobName());
        log.setStartTime(new Date());

        int urgeCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();
        try {
            // 只扫未判定（待抽样/抽样中/待判定）的批，超期天数随查询回填
            List<TSampLot> lots = tSampLotService.selectTSampLotList(new QueryWrapper<TSampLot>().lt("status", 3));
            log.setScanCount(lots.size());
            for (TSampLot lot : lots) {
                if (lot.getOverdueDays() == null || lot.getOverdueDays() <= 0) {
                    continue;
                }
                // 同一批同一天只自动催办一次（含人工催办），重复执行不产生重复记录
                if (urgedToday(lot.getId())) {
                    continue;
                }
                try {
                    tSampUrgeMapper.insert(buildUrge(lot));
                    urgeCount++;
                } catch (Exception e) {
                    // 单批失败不中断扫描，计入失败条数并记录错误
                    failCount++;
                    if (errorMsg.length() > 0) {
                        errorMsg.append("；");
                    }
                    errorMsg.append("检验批[").append(lot.getLotNo()).append("]生成催办失败：").append(e.getMessage());
                }
            }
        } catch (Exception e) {
            // 扫描本身失败（如查询异常），整个任务记为失败
            if (errorMsg.length() > 0) {
                errorMsg.append("；");
            }
            errorMsg.append("扫描超期检验批异常：").append(e.getMessage());
        }

        log.setUrgeCount(urgeCount);
        log.setFailCount(failCount);
        log.setStatus(errorMsg.length() > 0 ? 1 : 0);
        if (errorMsg.length() > 0) {
            log.setErrorMsg(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg.toString());
        }
        log.setEndTime(new Date());
        log.setCreateTime(new Date());
        tSampJobLogMapper.insert(log);
        return log;
    }

    /**
     * 组装一条自动催办记录：催办单号/催办次数规则与人工催办一致，
     * 定时扫描无登录人，催办人留空，以来源字段（1定时扫描）区分
     *
     * @param lot 超期检验批
     * @return 催办记录
     */
    private TSampUrge buildUrge(TSampLot lot) {
        TSampUrge urge = new TSampUrge();
        urge.setLotId(lot.getId());
        urge.setUrgeNo("CB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        urge.setUrgeCount(nextUrgeCount(lot.getId()));
        urge.setOverdueDays(lot.getOverdueDays());
        urge.setSource(1);
        urge.setStatus(0);
        urge.setDelFlag(0);
        urge.setUrgeTime(new Date());
        return urge;
    }

    /**
     * 该检验批今天是否已催办过（不限来源与催办人）
     *
     * @param lotId 检验批ID
     * @return true=今天已催办过
     */
    private boolean urgedToday(Long lotId) {
        Date dayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Integer count = tSampUrgeMapper.selectCount(new QueryWrapper<TSampUrge>()
                .eq("lot_id", lotId)
                .ge("urge_time", dayStart)
                .eq("del_flag", 0));
        return count != null && count > 0;
    }

    /**
     * 催办次数按批递增：取该批已有催办的最大次数 + 1
     *
     * @param lotId 检验批ID
     * @return 本次催办次数
     */
    private int nextUrgeCount(Long lotId) {
        List<TSampUrge> lastList = tSampUrgeMapper.selectList(new QueryWrapper<TSampUrge>()
                .select("urge_count")
                .eq("lot_id", lotId)
                .eq("del_flag", 0)
                .orderByDesc("urge_count")
                .last("limit 1"));
        if (!lastList.isEmpty() && lastList.get(0).getUrgeCount() != null) {
            return lastList.get(0).getUrgeCount() + 1;
        }
        return 1;
    }
}
