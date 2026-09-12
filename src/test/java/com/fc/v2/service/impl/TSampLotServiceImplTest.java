package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

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
 * 检验批修改-批量锁定单测：已判定(3)/已关闭(4)后批量不允许再改
 */
class TSampLotServiceImplTest {

    @Mock
    private TSampLotMapper tSampLotMapper;

    @Mock
    private TSampSchemeMapper tSampSchemeMapper;

    @Mock
    private TSampProductMapper tSampProductMapper;

    @InjectMocks
    private TSampLotServiceImpl tSampLotService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
    void 待判定_批量允许修改() {
        when(tSampLotMapper.selectOne(any())).thenReturn(dbLot(2, 500));
        when(tSampLotMapper.update(any(), any())).thenReturn(1);
        int rows = tSampLotService.updateTSampLot(formLot(9999));
        assertEquals(1, rows);
        verify(tSampLotMapper).update(any(), any());
    }

    @Test
    void 批量锁定状态判断() {
        assertFalse(tSampLotService.isBatchQtyLocked(dbLot(0, 500)));
        assertFalse(tSampLotService.isBatchQtyLocked(dbLot(1, 500)));
        assertFalse(tSampLotService.isBatchQtyLocked(dbLot(2, 500)));
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
}
