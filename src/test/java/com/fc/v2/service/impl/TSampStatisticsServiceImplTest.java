package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampRetestMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampRetest;

/**
 * 质量统计口径单测：合格率按已判定批算，一次检验通过率剔除走过复检流程的批
 */
class TSampStatisticsServiceImplTest {

    @Mock
    private TSampLotMapper tSampLotMapper;

    @Mock
    private TSampRetestMapper tSampRetestMapper;

    private TSampStatisticsServiceImpl tSampStatisticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tSampStatisticsService = new TSampStatisticsServiceImpl();
        ReflectionTestUtils.setField(tSampStatisticsService, "tSampLotMapper", tSampLotMapper);
        ReflectionTestUtils.setField(tSampStatisticsService, "tSampRetestMapper", tSampRetestMapper);
    }

    private Date day(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TSampLot lot(long id, Long productId, String productCode, String productName,
                         String conclude, String applyDate) {
        TSampLot lot = new TSampLot();
        lot.setId(id);
        lot.setProductId(productId);
        lot.setProductCode(productCode);
        lot.setProductName(productName);
        lot.setConclude(conclude);
        lot.setApplyDate(day(applyDate));
        return lot;
    }

    private TSampRetest retest(long lotId, int status) {
        TSampRetest retest = new TSampRetest();
        retest.setLotId(lotId);
        retest.setStatus(status);
        return retest;
    }

    @Test
    @SuppressWarnings("unchecked")
    void 按产品统计_合格率与一次通过率口径() {
        // 1 合格无复检 2 不合格无复检 3 复检改判合格(已完成) 4 不合格待复检 5 合格但复检单已作废
        List<TSampLot> lots = Arrays.asList(
                lot(1, 10L, "P-01", "产品甲", "合格", "2026-09-01"),
                lot(2, 10L, "P-01", "产品甲", "不合格", "2026-09-02"),
                lot(3, 10L, "P-01", "产品甲", "合格", "2026-09-03"),
                lot(4, 20L, "P-02", "产品乙", "不合格", "2026-09-04"),
                lot(5, 20L, "P-02", "产品乙", "合格", "2026-09-05"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        // 只有待复检(0)/已完成(1)的复检单算走过复检流程，已作废(2)的不计
        when(tSampRetestMapper.selectList(any())).thenReturn(Arrays.asList(
                retest(3, 1), retest(4, 0)));

        Map<String, Object> result = tSampStatisticsService.statisticsByProduct(null, null, null);

        // 汇总：5 批，合格 3，不合格 2，合格率 60%；一次通过只有 1、5 两批，40%
        assertEquals(5, result.get("lotCount"));
        assertEquals(3, result.get("passCount"));
        assertEquals(2, result.get("failCount"));
        assertEquals(new BigDecimal("60.00"), result.get("passRate"));
        assertEquals(new BigDecimal("40.00"), result.get("firstPassRate"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(2, rows.size());
        // 产品甲：3 批 2 合格，复检改判的 3 号批不算一次通过 → 一次通过率 1/3
        Map<String, Object> jia = rows.get(0);
        assertEquals(10L, jia.get("productId"));
        assertEquals(3, jia.get("lotCount"));
        assertEquals(2, jia.get("passCount"));
        assertEquals(1, jia.get("failCount"));
        assertEquals(new BigDecimal("66.67"), jia.get("passRate"));
        assertEquals(new BigDecimal("33.33"), jia.get("firstPassRate"));
        // 产品乙：作废复检单不影响一次通过判定
        Map<String, Object> yi = rows.get(1);
        assertEquals(2, yi.get("lotCount"));
        assertEquals(new BigDecimal("50.00"), yi.get("passRate"));
        assertEquals(new BigDecimal("50.00"), yi.get("firstPassRate"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 按产品统计_未挂产品的批归入未指定产品且产品ID置空() {
        List<TSampLot> lots = Arrays.asList(
                lot(1, null, null, null, "合格", "2026-09-01"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        when(tSampRetestMapper.selectList(any())).thenReturn(new ArrayList<TSampRetest>());

        Map<String, Object> result = tSampStatisticsService.statisticsByProduct(null, null, null);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("productId"));
        assertEquals("未指定产品", rows.get(0).get("productName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 月度趋势_分组升序且空月补零() {
        // 数据只在 1 月和 3 月，范围覆盖 1~3 月，2 月补零行
        List<TSampLot> lots = Arrays.asList(
                lot(2, 10L, "P-01", "产品甲", "不合格", "2026-03-02"),
                lot(1, 10L, "P-01", "产品甲", "合格", "2026-01-01"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        when(tSampRetestMapper.selectList(any())).thenReturn(new ArrayList<TSampRetest>());

        List<Map<String, Object>> rows = tSampStatisticsService.trendByMonth(
                day("2026-01-01"), day("2026-04-01"), null);

        assertEquals(3, rows.size());
        assertEquals("2026-01", rows.get(0).get("month"));
        assertEquals("2026-02", rows.get(1).get("month"));
        assertEquals("2026-03", rows.get(2).get("month"));
        // 空月：计数为 0，比率为 null（不能显示成 0%）
        assertEquals(0, rows.get(1).get("lotCount"));
        assertNull(rows.get(1).get("passRate"));
        assertNull(rows.get(1).get("firstPassRate"));
        // 有数据的月份正常计算
        assertEquals(1, rows.get(0).get("lotCount"));
        assertEquals(new BigDecimal("100.00"), rows.get(0).get("passRate"));
        assertEquals(new BigDecimal("100.00"), rows.get(0).get("firstPassRate"));
        assertEquals(new BigDecimal("0.00"), rows.get(2).get("passRate"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 月度趋势_时间范围单边时不补空月() {
        List<TSampLot> lots = Arrays.asList(
                lot(1, 10L, "P-01", "产品甲", "合格", "2026-01-01"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        when(tSampRetestMapper.selectList(any())).thenReturn(new ArrayList<TSampRetest>());

        // 只给开始时间，无法确定补零区间，只返回有数据的月份
        List<Map<String, Object>> rows = tSampStatisticsService.trendByMonth(
                day("2026-01-01"), null, null);
        assertEquals(1, rows.size());
        assertEquals("2026-01", rows.get(0).get("month"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 无数据时_不查复检单且比率为空() {
        when(tSampLotMapper.selectList(any())).thenReturn(new ArrayList<TSampLot>());

        Map<String, Object> result = tSampStatisticsService.statisticsByProduct(null, null, null);
        assertEquals(0, result.get("lotCount"));
        assertNull(result.get("passRate"));
        verify(tSampRetestMapper, never()).selectList(any());

        List<Map<String, Object>> trend = tSampStatisticsService.trendByMonth(null, null, null);
        assertTrue(trend.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 查询条件_状态删除标记报检日期产品都拼入() {
        when(tSampLotMapper.selectList(any())).thenReturn(new ArrayList<TSampLot>());

        tSampStatisticsService.statisticsByProduct(
                day("2026-01-01"), day("2026-04-01"), 99L);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampLotMapper).selectList(captor.capture());
        QueryWrapper wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        // 只统计未删除、已判定(含已关闭)的批，按报检日期和产品过滤
        assertTrue(sqlSegment.contains("del_flag"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("apply_date"));
        assertTrue(sqlSegment.contains("product_id"));
        // in 条件在 MP 里以集合形式存参数：已判定(3)/已关闭(4)都要在里面
        boolean hasStatusPair = false;
        for (Object value : wrapper.getParamNameValuePairs().values()) {
            if (value instanceof List && ((List<?>) value).contains(3) && ((List<?>) value).contains(4)) {
                hasStatusPair = true;
            }
        }
        assertTrue(hasStatusPair);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(99L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 复检单查询_只看待复检和已完成且未删除() {
        List<TSampLot> lots = Arrays.asList(
                lot(1, 10L, "P-01", "产品甲", "合格", "2026-09-01"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        when(tSampRetestMapper.selectList(any())).thenReturn(new ArrayList<TSampRetest>());

        tSampStatisticsService.trendByMonth(null, null, null);

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampRetestMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("del_flag"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("lot_id"));
    }
}
