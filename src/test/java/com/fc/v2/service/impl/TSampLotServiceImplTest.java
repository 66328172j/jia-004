package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.mapper.auto.TSampProductMapper;
import com.fc.v2.mapper.auto.TSampSchemeMapper;
import com.fc.v2.model.auto.TSampLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    }

    private TSampLot dbLot(int status, int batchQty) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setStatus(status);
        lot.setBatchQty(batchQty);
        return lot;
    }

    private TSampLot formLot(int batchQty) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setBatchQty(batchQty);
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
}
