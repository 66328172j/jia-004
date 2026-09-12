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
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.service.ITSampUrgeService;
import com.github.pagehelper.PageHelper;

/**
 * 超期催办Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampUrgeServiceImpl extends ServiceImpl<TSampUrgeMapper, TSampUrge> implements ITSampUrgeService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Override
    public TSampUrge selectTSampUrgeById(String id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<TSampUrge> selectTSampUrgeList(Wrapper<TSampUrge> queryWrapper) {
        QueryWrapper<TSampUrge> qw = new QueryWrapper<TSampUrge>();
        qw.eq("del_flag", 0);
        PageHelper.startPage(1, 10);
        return baseMapper.selectList(qw);
    }

    @Override
    public int calcOverdueDays(Long lotId) {
        TSampLot lot = tSampLotMapper.selectById(lotId);
        if (lot == null || lot.getApplyDate() == null) {
            return 0;
        }
        int requireDays = lot.getRequireDays() == null ? 7 : lot.getRequireDays();
        LocalDate requireDate = toLocalDate(lot.getApplyDate()).plusDays(requireDays);
        long days = ChronoUnit.DAYS.between(requireDate, LocalDate.now());
        return days > 0 ? (int) days - 1 : 0;
    }

    @Override
    public int insertTSampUrge(TSampUrge tSampUrge) {
        if (tSampUrge.getLotId() == null) {
            return 0;
        }
        TSampLot lot = tSampLotMapper.selectById(Long.valueOf(tSampUrge.getLotId()));
        if (lot == null) {
            return 0;
        }
        int overdueDays = calcOverdueDays(lot.getId());
        tSampUrge.setId(UUID.randomUUID().toString().replace("-", ""));
        tSampUrge.setUrgeNo("UR" + System.currentTimeMillis());
        tSampUrge.setUrgeCount(1);
        tSampUrge.setOverdueDays(overdueDays);
        tSampUrge.setUrgeBy(lot.getApplyBy());
        tSampUrge.setUrgeTime(new Date());
        tSampUrge.setSource(0);
        tSampUrge.setStatus(0);
        tSampUrge.setDelFlag(0);
        return baseMapper.insert(tSampUrge);
    }

    @Override
    public int updateTSampUrge(TSampUrge tSampUrge) {
        return baseMapper.updateById(tSampUrge);
    }

    @Override
    public int deleteTSampUrgeById(String id) {
        return baseMapper.deleteById(id);
    }

    private LocalDate toLocalDate(Date date) {
        return new java.sql.Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
    }
}
