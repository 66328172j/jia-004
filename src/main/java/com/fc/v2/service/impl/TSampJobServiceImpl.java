package com.fc.v2.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.model.auto.TSampJobLog;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.service.ITSampJobService;
import com.github.pagehelper.PageHelper;

/**
 * 定时任务Service实现（超期扫描）
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampJobServiceImpl extends ServiceImpl<TSampJobMapper, TSampJob> implements ITSampJobService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

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
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<TSampJobLog> selectTSampJobLogList(Wrapper<TSampJobLog> queryWrapper) {
        QueryWrapper<TSampJobLog> qw = new QueryWrapper<TSampJobLog>();
        PageHelper.startPage(1, 10);
        return tSampJobLogMapper.selectList(qw);
    }

    @Override
    public TSampJobLog runOverdueScan(String jobId) {
        TSampJob job = baseMapper.selectById(jobId);
        if (job == null) {
            return null;
        }

        TSampJobLog log = new TSampJobLog();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setJobName(job.getJobName());
        log.setStartTime(new Date());

        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        qw.eq("del_flag", 0);
        qw.ne("status", 2);
        List<TSampLot> lots = tSampLotMapper.selectList(qw);
        int scanCount = lots.size();
        log.setScanCount(scanCount);
        log.setUrgeCount(scanCount);

        for (TSampLot lot : lots) {
            int overdueDays = calcOverdueDays(lot);
            if (overdueDays <= 0) {
                continue;
            }
            TSampUrge urge = new TSampUrge();
            urge.setId(UUID.randomUUID().toString().replace("-", ""));
            urge.setLotId(String.valueOf(lot.getId()));
            urge.setUrgeNo("URS" + lot.getLotNo());
            urge.setOverdueDays(overdueDays);
            urge.setSource(1);
            urge.setStatus(0);
            urge.setDelFlag(0);
            urge.setUrgeTime(new Date());
            tSampUrgeMapper.insert(urge);
        }

        log.setEndTime(new Date());
        log.setStatus(0);
        tSampJobLogMapper.insert(log);
        return log;
    }

    private int calcOverdueDays(TSampLot lot) {
        if (lot.getApplyDate() == null) {
            return 0;
        }
        int requireDays = lot.getRequireDays() == null ? 7 : lot.getRequireDays();
        LocalDate requireDate = toLocalDate(lot.getApplyDate()).plusDays(requireDays);
        long days = ChronoUnit.DAYS.between(requireDate, LocalDate.now());
        return days > 0 ? (int) days : 0;
    }

    private LocalDate toLocalDate(Date date) {
        return new java.sql.Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
    }
}
