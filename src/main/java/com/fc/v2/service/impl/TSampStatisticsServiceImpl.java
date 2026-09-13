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
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampRetestMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;
import com.fc.v2.service.ITSampStatisticsService;

/**
 * 质量统计Service实现
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TSampStatisticsServiceImpl implements ITSampStatisticsService {

    /** 已判定 */
    private static final int STATUS_JUDGED = 3;
    /** 已关闭 */
    private static final int STATUS_CLOSED = 4;

    /** 复检单待复检 */
    private static final int RETEST_PENDING = 0;
    /** 复检单已完成 */
    private static final int RETEST_FINISHED = 1;

    /** 未挂产品的检验批分组用占位ID */
    private static final Long NO_PRODUCT_ID = Long.valueOf(-1L);
    private static final String NO_PRODUCT_NAME = "未指定产品";

    /** 零填充最多覆盖的月份跨度（100年），防止异常入参生成海量空行 */
    private static final int MAX_FILL_MONTHS = 1200;

    @Autowired
    private TSampLotMapper tSampLotMapper;

    @Autowired
    private TSampRetestMapper tSampRetestMapper;

    @Override
    public Map<String, Object> statisticsByProduct(Date beginTime, Date endTime, Long productId) {
        List<TSampLot> lots = listJudgedLots(beginTime, endTime, productId);
        Set<Long> retestLotIds = listRetestLotIds(lots);

        Map<Long, List<TSampLot>> byProduct = new LinkedHashMap<Long, List<TSampLot>>();
        for (TSampLot lot : lots) {
            Long key = lot.getProductId() == null ? NO_PRODUCT_ID : lot.getProductId();
            List<TSampLot> group = byProduct.get(key);
            if (group == null) {
                group = new ArrayList<TSampLot>();
                byProduct.put(key, group);
            }
            group.add(lot);
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Long, List<TSampLot>> entry : byProduct.entrySet()) {
            List<TSampLot> group = entry.getValue();
            Map<String, Object> row = buildStatRow(group, retestLotIds);
            row.put("productId", NO_PRODUCT_ID.equals(entry.getKey()) ? null : entry.getKey());
            row.put("productCode", group.get(0).getProductCode());
            row.put("productName", group.get(0).getProductName() == null
                    ? NO_PRODUCT_NAME : group.get(0).getProductName());
            rows.add(row);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("rows", rows);
        // 汇总行按全部批直接算，不用各产品率平均，避免小批量产品权重相同的失真
        result.putAll(buildStatRow(lots, retestLotIds));
        return result;
    }

    @Override
    public List<Map<String, Object>> trendByMonth(Date beginTime, Date endTime, Long productId) {
        List<TSampLot> lots = listJudgedLots(beginTime, endTime, productId);
        Set<Long> retestLotIds = listRetestLotIds(lots);

        // TreeMap 保证月份按时间先后升序
        Map<String, List<TSampLot>> byMonth = new TreeMap<String, List<TSampLot>>();
        for (TSampLot lot : lots) {
            String key = monthKey(lot.getApplyDate());
            List<TSampLot> group = byMonth.get(key);
            if (group == null) {
                group = new ArrayList<TSampLot>();
                byMonth.put(key, group);
            }
            group.add(lot);
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (String month : fillMonths(beginTime, endTime, byMonth.keySet())) {
            Map<String, Object> row = buildStatRow(byMonth.get(month), retestLotIds);
            row.put("month", month);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 查参与统计的检验批：未删除、已判定（已判定/已关闭）、有报检日期，按报检日期/产品过滤
     */
    private List<TSampLot> listJudgedLots(Date beginTime, Date endTime, Long productId) {
        QueryWrapper<TSampLot> qw = new QueryWrapper<TSampLot>();
        qw.eq("del_flag", 0);
        qw.in("status", STATUS_JUDGED, STATUS_CLOSED);
        qw.isNotNull("apply_date");
        if (beginTime != null) {
            qw.ge("apply_date", beginTime);
        }
        if (endTime != null) {
            qw.lt("apply_date", endTime);
        }
        if (productId != null) {
            qw.eq("product_id", productId);
        }
        qw.orderByAsc("apply_date");
        return tSampLotMapper.selectList(qw);
    }

    /**
     * 查这批检验批里走过复检流程的批次ID：存在待复检或已完成复检单即算非一次通过，
     * 已作废/已删除的复检单不计
     */
    private Set<Long> listRetestLotIds(List<TSampLot> lots) {
        Set<Long> retestLotIds = new java.util.HashSet<Long>();
        if (lots.isEmpty()) {
            return retestLotIds;
        }
        List<Long> lotIds = new ArrayList<Long>();
        for (TSampLot lot : lots) {
            lotIds.add(lot.getId());
        }
        List<TSampRetest> retests = tSampRetestMapper.selectList(new QueryWrapper<TSampRetest>()
                .select("DISTINCT lot_id")
                .eq("del_flag", 0)
                .in("status", RETEST_PENDING, RETEST_FINISHED)
                .in("lot_id", lotIds));
        for (TSampRetest retest : retests) {
            if (retest.getLotId() != null) {
                retestLotIds.add(retest.getLotId());
            }
        }
        return retestLotIds;
    }

    /**
     * 构造一行统计：group 为 null/空时返回各计数为 0、比率为 null 的空月行
     */
    private Map<String, Object> buildStatRow(List<TSampLot> group, Set<Long> retestLotIds) {
        Map<String, Object> row = new HashMap<String, Object>();
        if (group == null || group.isEmpty()) {
            row.put("lotCount", 0);
            row.put("passCount", 0);
            row.put("failCount", 0);
            row.put("passRate", null);
            row.put("firstPassRate", null);
            return row;
        }
        int lotCount = group.size();
        int failCount = 0;
        int firstPassCount = 0;
        for (TSampLot lot : group) {
            boolean fail = "不合格".equals(lot.getConclude());
            if (fail) {
                failCount++;
            }
            // 一次通过：当前判定合格且没有走过复检流程（复检改判合格的不算）
            if (!fail && !retestLotIds.contains(lot.getId())) {
                firstPassCount++;
            }
        }
        int passCount = lotCount - failCount;
        row.put("lotCount", lotCount);
        row.put("passCount", passCount);
        row.put("failCount", failCount);
        row.put("passRate", rate(passCount, lotCount));
        row.put("firstPassRate", rate(firstPassCount, lotCount));
        return row;
    }

    /**
     * 入参给了完整时间范围时补齐没有数据的月份，趋势线才不会跳过空月；
     * 只给了单边或跨度异常时退化为只返回有数据的月份
     */
    private List<String> fillMonths(Date beginTime, Date endTime, Set<String> dataMonths) {
        List<String> months = new ArrayList<String>();
        if (beginTime == null || endTime == null) {
            months.addAll(dataMonths);
            return months;
        }
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(beginTime);
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);

        Calendar last = Calendar.getInstance();
        last.setTime(endTime);
        last.add(Calendar.MILLISECOND, -1);

        int guard = 0;
        while (!cursor.after(last) && guard < MAX_FILL_MONTHS) {
            months.add(monthKey(cursor.getTime()));
            cursor.add(Calendar.MONTH, 1);
            guard++;
        }
        return months;
    }

    /** 月份键 yyyy-MM，补零，前端可直接展示与排序 */
    private String monthKey(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        return year + "-" + (month < 10 ? "0" + month : String.valueOf(month));
    }

    /** 百分比保留两位小数；分母为 0 时返回 null（无数据不显示 0% 误导） */
    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }
}
