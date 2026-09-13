package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampQueryLogMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampQueryLog;
import com.fc.v2.model.auto.TSampScanVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 移动端扫码查询单测：脱敏视图、未命中明确提示、每次查询落记录、落库失败不影响主流程
 */
class TSampScanServiceImplTest {

    @Mock
    private TSampLotMapper tSampLotMapper;

    @Mock
    private TSampQueryLogMapper tSampQueryLogMapper;

    private TSampScanServiceImpl tSampScanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tSampScanService = new TSampScanServiceImpl();
        ReflectionTestUtils.setField(tSampScanService, "tSampLotMapper", tSampLotMapper);
        ReflectionTestUtils.setField(tSampScanService, "tSampQueryLogMapper", tSampQueryLogMapper);
    }

    private TSampLot lot() {
        TSampLot lot = new TSampLot();
        lot.setId(1001L);
        lot.setLotNo("LOT-20260913-01");
        lot.setProductCode("P-001");
        lot.setProductName("测试产品");
        // 内部字段：即使有值也不允许出现在对外结果里
        lot.setBatchQty(500);
        lot.setSchemeCode("J");
        lot.setAql(new java.math.BigDecimal("2.50"));
        lot.setSampleSize(50);
        lot.setAcceptCount(3);
        lot.setRejectCount(4);
        lot.setDefectCount(1);
        lot.setPassRate(new java.math.BigDecimal("98.00"));
        lot.setStatus(3);
        lot.setConclude("合格");
        lot.setGradeCode("A");
        lot.setGradeName("一等品");
        lot.setDelFlag(0);
        return lot;
    }

    @Test
    void 命中批号_返回脱敏视图_只含六个客户可见字段() throws Exception {
        when(tSampLotMapper.selectOne(any())).thenReturn(lot());

        Map<String, Object> result = tSampScanService.scanQuery("LOT-20260913-01", "WECHAT");

        assertEquals(true, result.get("hit"));
        Object data = result.get("data");
        // 必须是脱敏 VO，不能把库表实体 TSampLot（含 AQL/样本量/接收数/拒收数等）直接丢给客户
        assertTrue(data instanceof TSampScanVo);
        TSampScanVo vo = (TSampScanVo) data;
        assertEquals("LOT-20260913-01", vo.getLotNo());
        assertEquals("P-001", vo.getProductCode());
        assertEquals("测试产品", vo.getProductName());
        assertEquals(3, vo.getStatus());
        assertEquals("合格", vo.getConclude());
        assertEquals("一等品", vo.getGradeName());

        Set<String> voFields = new HashSet<String>();
        for (Field field : TSampScanVo.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                voFields.add(field.getName());
            }
        }
        assertEquals(new HashSet<String>(Arrays.asList(
                "lotNo", "productCode", "productName", "status", "conclude", "gradeName",
                "serialVersionUID")), voFields);
        assertFalse(voFields.contains("aql"));
        assertFalse(voFields.contains("sampleSize"));
        assertFalse(voFields.contains("acceptCount"));
        assertFalse(voFields.contains("rejectCount"));
        assertFalse(voFields.contains("defectCount"));
        assertFalse(voFields.contains("passRate"));
    }

    @Test
    void 命中查询_只查正常批_查询条件带del_flag() {
        when(tSampLotMapper.selectOne(any())).thenReturn(lot());

        tSampScanService.scanQuery(" LOT-20260913-01 ", "H5");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<TSampLot>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampLotMapper).selectOne(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        // 按批号精确查，且逻辑删除的批按不存在处理
        assertTrue(sqlSegment.contains("lot_no"));
        assertTrue(sqlSegment.contains("del_flag"));
    }

    @Test
    void 未命中批号_hit为false_给明确提示_不抛异常() {
        when(tSampLotMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = assertDoesNotThrow(
                () -> tSampScanService.scanQuery("LOT-404", "H5"));

        assertEquals(false, result.get("hit"));
        assertNull(result.get("data"));
        assertTrue(String.valueOf(result.get("message")).contains("LOT-404"));
        assertTrue(String.valueOf(result.get("message")).contains("未查询到"));
    }

    @Test
    void 空批号_不查库_提示输入批号() {
        Map<String, Object> result = tSampScanService.scanQuery("   ", "H5");

        assertEquals(false, result.get("hit"));
        assertNull(result.get("data"));
        assertTrue(String.valueOf(result.get("message")).contains("请输入"));
        verify(tSampLotMapper, never()).selectOne(any());
    }

    @Test
    void null批号_不抛异常() {
        Map<String, Object> result = assertDoesNotThrow(
                () -> tSampScanService.scanQuery(null, null));
        assertEquals(false, result.get("hit"));
    }

    @Test
    void 命中查询_落查询记录_记录批号来源时间与命中标志() {
        when(tSampLotMapper.selectOne(any())).thenReturn(lot());

        tSampScanService.scanQuery("LOT-20260913-01", "APP");

        TSampQueryLog queryLog = captureQueryLog();
        assertEquals("LOT-20260913-01", queryLog.getLotNo());
        assertEquals("1001", queryLog.getLotId());
        assertEquals("APP", queryLog.getQuerySource());
        assertEquals(1, queryLog.getResultFlag());
        assertTrue(queryLog.getId() != null && !queryLog.getId().isEmpty());
        assertTrue(queryLog.getQueryTime() != null);
        assertTrue(queryLog.getCreateTime() != null);
    }

    @Test
    void 未命中查询_也要落记录_命中标志为0_lotId为空() {
        when(tSampLotMapper.selectOne(any())).thenReturn(null);

        tSampScanService.scanQuery("LOT-404", "WECHAT");

        TSampQueryLog queryLog = captureQueryLog();
        assertEquals("LOT-404", queryLog.getLotNo());
        assertNull(queryLog.getLotId());
        assertEquals("WECHAT", queryLog.getQuerySource());
        assertEquals(0, queryLog.getResultFlag());
    }

    @Test
    void 来源为空_默认H5() {
        when(tSampLotMapper.selectOne(any())).thenReturn(null);

        tSampScanService.scanQuery("LOT-404", "  ");

        assertEquals("H5", captureQueryLog().getQuerySource());
    }

    @Test
    void 批号带空格_trim后再查和落库() {
        when(tSampLotMapper.selectOne(any())).thenReturn(null);

        tSampScanService.scanQuery("  LOT-404  ", "H5");

        assertEquals("LOT-404", captureQueryLog().getLotNo());
    }

    @Test
    void 查询记录落库失败_不影响查询主流程() {
        when(tSampLotMapper.selectOne(any())).thenReturn(lot());
        when(tSampQueryLogMapper.insert(any())).thenThrow(new RuntimeException("模拟数据库不可用"));

        Map<String, Object> result = assertDoesNotThrow(
                () -> tSampScanService.scanQuery("LOT-20260913-01", "H5"));

        assertEquals(true, result.get("hit"));
        assertTrue(result.get("data") instanceof TSampScanVo);
    }

    @Test
    void 查询记录列表_透传mapper结果() {
        TSampQueryLog log1 = new TSampQueryLog();
        log1.setLotNo("LOT-1");
        when(tSampQueryLogMapper.selectList(any())).thenReturn(Arrays.asList(log1));

        List<TSampQueryLog> list = tSampScanService.selectQueryLogList(
                new QueryWrapper<TSampQueryLog>().like("lot_no", "LOT"));

        assertEquals(1, list.size());
        assertSame(log1, list.get(0));
    }

    @Test
    void 批量删除查询记录_按逗号拆分() {
        when(tSampQueryLogMapper.deleteBatchIds(any())).thenReturn(2);

        int rows = tSampScanService.deleteQueryLogByIds("id1,id2");

        assertEquals(2, rows);
        verify(tSampQueryLogMapper).deleteBatchIds(Arrays.asList("id1", "id2"));
    }

    @Test
    void 批量删除_空参数_不调库() {
        assertEquals(0, tSampScanService.deleteQueryLogByIds("  "));
        verify(tSampQueryLogMapper, never()).deleteBatchIds(any());
    }

    private TSampQueryLog captureQueryLog() {
        ArgumentCaptor<TSampQueryLog> captor = ArgumentCaptor.forClass(TSampQueryLog.class);
        verify(tSampQueryLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}
