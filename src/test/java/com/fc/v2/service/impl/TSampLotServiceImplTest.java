package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 检验批修改-批量锁定单测：抽样开始（抽样中及之后状态）后批量不允许再改
 */
class TSampLotServiceImplTest {

    @Mock
    private TSampLotMapper tSampLotMapper;

    @Mock
    private TSampSchemeMapper tSampSchemeMapper;

    @Mock
    private TSampProductMapper tSampProductMapper;

    private TSampLotServiceImpl tSampLotService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 不用 @InjectMocks：多个 BaseMapper 类型的 mock 会让 baseMapper 注入产生歧义，显式按字段名注入
        tSampLotService = new TSampLotServiceImpl();
        ReflectionTestUtils.setField(tSampLotService, "baseMapper", tSampLotMapper);
        ReflectionTestUtils.setField(tSampLotService, "tSampSchemeMapper", tSampSchemeMapper);
        ReflectionTestUtils.setField(tSampLotService, "tSampProductMapper", tSampProductMapper);
        // 放行场景会走派生字段填充，方案匹配不到时不影响主流程
        lenient().when(tSampSchemeMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 默认产品档案存在且启用，各用例可按需覆盖
        lenient().when(tSampProductMapper.selectOne(any())).thenReturn(product(1L, 0));
    }

    private TSampProduct product(Long id, int status) {
        TSampProduct product = new TSampProduct();
        product.setId(id);
        product.setCode("P-001");
        product.setName("测试产品");
        product.setStatus(status);
        product.setDelFlag(0);
        return product;
    }

    private TSampLot dbLot(int status, int batchQty) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setStatus(status);
        lot.setBatchQty(batchQty);
        lot.setProductId(1L);
        return lot;
    }

    private TSampLot formLot(int batchQty) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setBatchQty(batchQty);
        lot.setProductId(1L);
        return lot;
    }

    @Test
    void 已判定_批量被改_拒绝更新() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(3, 500));
        int rows = tSampLotService.updateTSampLot(formLot(9999));
        assertEquals(0, rows);
        verify(tSampLotMapper, never()).update(any(), any());
    }

    @Test
    void 已关闭_批量被改_拒绝更新() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(4, 500));
        int rows = tSampLotService.updateTSampLot(formLot(9999));
        assertEquals(0, rows);
        verify(tSampLotMapper, never()).update(any(), any());
    }

    @Test
    void 已判定_批量未变_其他字段允许修改() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(3, 500));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        int rows = tSampLotService.updateTSampLot(formLot(500));
        assertEquals(1, rows);
        verify(tSampLotMapper).update(any(), any());
    }

    @Test
    void 已判定_编辑保存_抽样方案字段冻结不重刷() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(3, 500));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        TSampLot form = formLot(500);
        tSampLotService.updateTSampLot(form);
        // 方案四字段置 null，MyBatis-Plus 更新时跳过这些列，保持库中已判定的方案
        assertNull(form.getSchemeCode());
        assertNull(form.getSampleSize());
        assertNull(form.getAcceptCount());
        assertNull(form.getRejectCount());
    }

    @Test
    void 待抽样_批量允许修改() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(0, 500));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        int rows = tSampLotService.updateTSampLot(formLot(9999));
        assertEquals(1, rows);
        verify(tSampLotMapper).update(any(), any());
    }

    @Test
    void 批量锁定状态判断() {
        // 待抽样(0)批量还可改；抽样开始(1)起样本已按方案生成，批量即锁定
        assertFalse(tSampLotService.isBatchQtyLocked(dbLot(0, 500)));
        assertTrue(tSampLotService.isBatchQtyLocked(dbLot(1, 500)));
        assertTrue(tSampLotService.isBatchQtyLocked(dbLot(2, 500)));
        assertTrue(tSampLotService.isBatchQtyLocked(dbLot(3, 500)));
        assertTrue(tSampLotService.isBatchQtyLocked(dbLot(4, 500)));
    }

    @Test
    void 新增_批量为0_拒绝入库() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(formLot(0)));
        assertTrue(ex.getMessage().contains("正整数"));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 新增_批量为负数_拒绝入库() {
        assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(formLot(-10)));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 新增_批量为空_拒绝入库() {
        TSampLot form = new TSampLot();
        assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(form));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 修改_批量改为0_拒绝更新() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(0, 500));
        assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.updateTSampLot(formLot(0)));
        verify(tSampLotMapper, never()).update(any(), any());
    }

    @Test
    void 新增_合法正整数批量_允许入库() {
        when(tSampLotMapper.insert(any())).thenReturn(1);
        int rows = tSampLotService.insertTSampLot(formLot(500));
        assertEquals(1, rows);
        verify(tSampLotMapper).insert(any());
    }

    @Test
    void 新增_停用产品_拒绝报检() {
        when(tSampProductMapper.selectOne(any())).thenReturn(product(1L, 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(formLot(500)));
        assertTrue(ex.getMessage().contains("已停用"));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 新增_产品不存在_拒绝报检() {
        when(tSampProductMapper.selectOne(any())).thenReturn(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(formLot(500)));
        assertTrue(ex.getMessage().contains("不存在"));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 新增_产品为空_拒绝报检() {
        TSampLot form = formLot(500);
        form.setProductId(null);
        assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.insertTSampLot(form));
        verify(tSampLotMapper, never()).insert(any());
    }

    @Test
    void 新增_页面乱填产品编号_以档案为准覆盖() {
        when(tSampLotMapper.insert(any())).thenReturn(1);
        TSampLot form = formLot(500);
        form.setProductCode("HACK-999");
        form.setProductName("乱填的名称");
        tSampLotService.insertTSampLot(form);
        assertEquals("P-001", form.getProductCode());
        assertEquals("测试产品", form.getProductName());
        verify(tSampLotMapper).insert(any());
    }

    @Test
    void 修改_换绑停用产品_拒绝更新() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(0, 500));
        when(tSampProductMapper.selectOne(any())).thenReturn(product(2L, 1));
        TSampLot form = formLot(500);
        form.setProductId(2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampLotService.updateTSampLot(form));
        assertTrue(ex.getMessage().contains("已停用"));
        verify(tSampLotMapper, never()).update(any(), any());
    }

    @Test
    void 修改_未换产品_产品后续停用不拦截() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(0, 500));
        when(tSampProductMapper.selectOne(any())).thenReturn(product(1L, 1));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        int rows = tSampLotService.updateTSampLot(formLot(500));
        assertEquals(1, rows);
        verify(tSampLotMapper).update(any(), any());
    }

    @Test
    void 已判定_编辑保存_产品字段冻结不换绑() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(3, 500));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        TSampLot form = formLot(500);
        form.setProductId(2L);
        form.setProductCode("HACK-999");
        tSampLotService.updateTSampLot(form);
        // 产品字段置 null，MyBatis-Plus 更新时跳过这些列，保持库中已判定的产品
        assertNull(form.getProductId());
        assertNull(form.getProductCode());
        assertNull(form.getProductName());
        assertNull(form.getAql());
    }

    private TSampLot lotForOverdue(int status, Integer requireDays, Date applyDate) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setStatus(status);
        lot.setRequireDays(requireDays);
        lot.setApplyDate(applyDate);
        return lot;
    }

    /**
     * n 天前中午 12 点（避开零点附近跑测试时的日期边界）
     */
    private Date daysAgo(int days) {
        return java.sql.Timestamp.valueOf(LocalDate.now().minusDays(days).atTime(12, 0));
    }

    @Test
    void 超期计算_刚好到期当天_不算超期() {
        // 报检 7 天前、要求 7 天完成：今天刚好到期，不算超期
        assertEquals(0, tSampLotService.calcOverdueDays(lotForOverdue(2, 7, daysAgo(7))));
    }

    @Test
    void 超期计算_超过要求天数_返回超期天数() {
        // 报检 12 天前、要求 7 天完成：超期 5 天
        assertEquals(5, tSampLotService.calcOverdueDays(lotForOverdue(2, 7, daysAgo(12))));
    }

    @Test
    void 超期计算_未到期_返回0() {
        assertEquals(0, tSampLotService.calcOverdueDays(lotForOverdue(2, 7, daysAgo(3))));
    }

    @Test
    void 超期计算_已判定_不再超期() {
        assertEquals(0, tSampLotService.calcOverdueDays(lotForOverdue(3, 7, daysAgo(30))));
    }

    @Test
    void 超期计算_已关闭_不再超期() {
        assertEquals(0, tSampLotService.calcOverdueDays(lotForOverdue(4, 7, daysAgo(30))));
    }

    @Test
    void 超期计算_未设要求天数_按默认7天() {
        // 报检 10 天前、未设要求完成天数：按默认 7 天，超期 3 天
        assertEquals(3, tSampLotService.calcOverdueDays(lotForOverdue(2, null, daysAgo(10))));
    }

    @Test
    void 超期计算_无报检日期_按创建时间起算() {
        TSampLot lot = lotForOverdue(2, 7, null);
        lot.setCreateTime(daysAgo(9));
        assertEquals(2, tSampLotService.calcOverdueDays(lot));
    }
}
