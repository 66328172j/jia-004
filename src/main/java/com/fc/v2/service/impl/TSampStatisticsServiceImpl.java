package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.service.ITSampStatisticsService;

/**
 * 质量统计Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampStatisticsServiceImpl implements ITSampStatisticsService {

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Override
    public List<Map<String, Object>> trendByMonth(Date beginTime, Date endTime) {
        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        if (beginTime != null) {
            qw.ge("apply_date", beginTime);
        }
        if (endTime != null) {
            qw.lt("apply_date", endTime);
        }
        List<TSampLot> lots = tSampLotMapper.selectList(qw);
        if (lots.isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }

        Map<String, List<TSampLot>> byMonth = new java.util.TreeMap<String, List<TSampLot>>();
        for (TSampLot lot : lots) {
            Calendar c = Calendar.getInstance();
            c.setTime(lot.getApplyDate());
            String key = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1);
            List<TSampLot> group = byMonth.get(key);
            if (group == null) {
                group = new ArrayList<TSampLot>();
                byMonth.put(key, group);
            }
            group.add(lot);
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<TSampLot>> entry : byMonth.entrySet()) {
            Map<String, Object> row = buildStatRow(entry.getValue());
            row.put("month", entry.getKey());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Map<String, Object> statisticsByProduct(Date beginTime, Date endTime) {
        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        if (beginTime != null) {
            qw.ge("apply_date", beginTime);
        }
        if (endTime != null) {
            qw.lt("apply_date", endTime);
        }
        List<TSampLot> lots = tSampLotMapper.selectList(qw);

        Map<Long, List<TSampLot>> byProduct = new LinkedHashMap<Long, List<TSampLot>>();
        for (TSampLot lot : lots) {
            Long key = lot.getProductId() == null ? Long.valueOf(-1L) : lot.getProductId();
            List<TSampLot> group = byProduct.get(key);
            if (group == null) {
                group = new ArrayList<TSampLot>();
                byProduct.put(key, group);
            }
            group.add(lot);
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        int sumRate = 0;
        for (Map.Entry<Long, List<TSampLot>> entry : byProduct.entrySet()) {
            List<TSampLot> group = entry.getValue();
            Map<String, Object> row = buildStatRow(group);
            row.put("productId", entry.getKey());
            row.put("productName", group.get(0).getProductName());
            rows.add(row);
            sumRate += ((BigDecimal) row.get("passRate")).intValue();
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total", lots.size());
        result.put("rows", rows);
        result.put("avgPassRate", round2(sumRate / rows.size()));
        return result;
    }

    private Map<String, Object> buildStatRow(List<TSampLot> group) {
        int lotCount = group.size();
        int failCount = 0;
        for (TSampLot lot : group) {
            if ("不合格".equals(lot.getConclude())) {
                failCount++;
            }
        }
        int passCount = lotCount - failCount;
        BigDecimal passRate = round2(passCount * 100.0 / lotCount);

        Map<String, Object> row = new HashMap<String, Object>();
        row.put("lotCount", lotCount);
        row.put("passCount", passCount);
        row.put("failCount", failCount);
        row.put("passRate", passRate);
        row.put("firstPassRate", passRate);
        return row;
    }

    private BigDecimal round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
