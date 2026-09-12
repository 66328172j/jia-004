package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.service.ITSampLotService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.ThreadContext;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超期催办单测：未超期不允许催办、催办次数按批递增、
 * 催办人取当前登录人、同人同日同批不重复记录
 */
class TSampUrgeServiceImplTest {

    @Mock
    private TSampUrgeMapper tSampUrgeMapper;

    @Mock
    private ITSampLotService tSampLotService;

    @InjectMocks
    private TSampUrgeServiceImpl tSampUrgeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 催办人取当前登录人：mock Shiro 登录上下文，登录人固定为 zhangsan
        TSysUser loginUser = new TSysUser();
        loginUser.setUsername("zhangsan");
        Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(loginUser);
        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.createSubject(any())).thenReturn(subject);
        SecurityUtils.setSecurityManager(securityManager);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
        SecurityUtils.setSecurityManager(null);
    }

    private TSampLot lot() {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setLotNo("LOT-001");
        lot.setStatus(2);
        return lot;
    }

    private TSampUrge formUrge() {
        TSampUrge urge = new TSampUrge();
        urge.setLotId(1L);
        return urge;
    }

    /**
     * 放行到落库前的公共 mock：检验批存在且当前超期 5 天，今天尚未催办过
     */
    private void mockOverdueLot() {
        when(tSampLotService.selectTSampLotById(1L)).thenReturn(lot());
        when(tSampLotService.calcOverdueDays(any())).thenReturn(5);
        when(tSampUrgeMapper.selectCount(any())).thenReturn(0);
    }

    @Test
    void 催办_检验批为空_拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampUrgeService.insertTSampUrge(new TSampUrge()));
        assertTrue(ex.getMessage().contains("检验批不能为空"));
        verify(tSampUrgeMapper, never()).insert(any());
    }

    @Test
    void 催办_检验批不存在_拒绝() {
        when(tSampLotService.selectTSampLotById(1L)).thenReturn(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampUrgeService.insertTSampUrge(formUrge()));
        assertTrue(ex.getMessage().contains("不存在或已删除"));
        verify(tSampUrgeMapper, never()).insert(any());
    }

    @Test
    void 催办_未超期_拒绝() {
        when(tSampLotService.selectTSampLotById(1L)).thenReturn(lot());
        when(tSampLotService.calcOverdueDays(any())).thenReturn(0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampUrgeService.insertTSampUrge(formUrge()));
        assertTrue(ex.getMessage().contains("未超期"));
        verify(tSampUrgeMapper, never()).insert(any());
    }

    @Test
    void 催办_今天已催办过_拒绝重复() {
        mockOverdueLot();
        // 同一催办人今天对该批已有催办记录
        when(tSampUrgeMapper.selectCount(any())).thenReturn(1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampUrgeService.insertTSampUrge(formUrge()));
        assertTrue(ex.getMessage().contains("请勿重复催办"));
        verify(tSampUrgeMapper, never()).insert(any());
    }

    @Test
    void 催办_首次_单号次数催办人时间超期天数落库() {
        mockOverdueLot();
        when(tSampUrgeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampUrgeMapper.insert(any())).thenReturn(1);
        TSampUrge urge = formUrge();
        int rows = tSampUrgeService.insertTSampUrge(urge);
        assertEquals(1, rows);
        // 催办单号后端生成，次数从 1 开始，催办人取当前登录人，超期天数取催办时计算值
        assertTrue(urge.getUrgeNo().startsWith("CB-"));
        assertEquals(1, urge.getUrgeCount());
        assertEquals("zhangsan", urge.getUrgeBy());
        assertEquals(5, urge.getOverdueDays());
        assertNotNull(urge.getUrgeTime());
        assertEquals(0, urge.getDelFlag());
        verify(tSampUrgeMapper).insert(any());
    }

    @Test
    void 催办_再次_次数按批递增() {
        mockOverdueLot();
        // 该批已有第 2 次催办，本次应为第 3 次
        TSampUrge last = new TSampUrge();
        last.setUrgeCount(2);
        when(tSampUrgeMapper.selectList(any())).thenReturn(Collections.singletonList(last));
        when(tSampUrgeMapper.insert(any())).thenReturn(1);
        TSampUrge urge = formUrge();
        tSampUrgeService.insertTSampUrge(urge);
        assertEquals(3, urge.getUrgeCount());
    }

    @Test
    void 催办_页面乱传单号次数_以后端生成值覆盖() {
        mockOverdueLot();
        when(tSampUrgeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampUrgeMapper.insert(any())).thenReturn(1);
        TSampUrge urge = formUrge();
        urge.setUrgeNo("HACK-001");
        urge.setUrgeCount(99);
        urge.setUrgeBy("lisi");
        urge.setOverdueDays(0);
        tSampUrgeService.insertTSampUrge(urge);
        assertTrue(urge.getUrgeNo().startsWith("CB-"));
        assertEquals(1, urge.getUrgeCount());
        assertEquals("zhangsan", urge.getUrgeBy());
        assertEquals(5, urge.getOverdueDays());
    }
}
