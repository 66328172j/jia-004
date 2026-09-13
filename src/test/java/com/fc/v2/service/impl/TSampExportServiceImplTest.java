package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.conf.V2Config;
import com.fc.v2.mapper.auto.TSampExportMapper;
import com.fc.v2.mapper.auto.TSampLotMapper;
import com.fc.v2.model.auto.TSampExport;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSysUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 检验台账导出单测：起止日期必填且开始不晚于结束、导出人取当前登录人、
 * 检验类型与时间范围拼入查询条件、结束日期含当天（上界取次日零点）、
 * CSV 带 BOM 中文表头不乱码、无数据也出只有表头的文件、含逗号字段加引号转义
 */
class TSampExportServiceImplTest {

    @Mock
    private TSampExportMapper tSampExportMapper;

    @Mock
    private TSampLotMapper tSampLotMapper;

    @Mock
    private V2Config v2Config;

    private TSampExportServiceImpl tSampExportService;

    /** 导出文件写到临时目录，跑完即清 */
    @TempDir
    File tempDir;

    private Subject subject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 不用 @InjectMocks：多个 BaseMapper 类型的 mock 会让 baseMapper 注入产生歧义，显式按字段名注入
        tSampExportService = new TSampExportServiceImpl();
        ReflectionTestUtils.setField(tSampExportService, "baseMapper", tSampExportMapper);
        ReflectionTestUtils.setField(tSampExportService, "tSampLotMapper", tSampLotMapper);
        ReflectionTestUtils.setField(tSampExportService, "v2Config", v2Config);
        // 导出人取当前登录人：mock Shiro 登录上下文，登录人固定为 zhangsan
        TSysUser loginUser = new TSysUser();
        loginUser.setUsername("zhangsan");
        subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(loginUser);
        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.createSubject(any())).thenReturn(subject);
        SecurityUtils.setSecurityManager(securityManager);
        // 导出目录指到测试临时目录
        when(v2Config.getProfile()).thenReturn(tempDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        ThreadContext.remove();
        SecurityUtils.setSecurityManager(null);
    }

    private Date day(String yyyyMMdd) {
        return java.sql.Date.valueOf(yyyyMMdd);
    }

    private TSampLot lot(String lotNo, String productName) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setLotNo(lotNo);
        lot.setProductName(productName);
        lot.setBatchQty(100);
        lot.setSchemeCode("E");
        lot.setAql(new BigDecimal("2.50"));
        lot.setSampleSize(13);
        lot.setDefectCount(1);
        lot.setPassRate(new BigDecimal("92.31"));
        lot.setConclude("合格");
        lot.setGradeName("一等品");
        return lot;
    }

    @Test
    void 导出_开始日期为空_拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampExportService.exportLedger(null, day("2026-09-30"), null));
        assertTrue(ex.getMessage().contains("开始日期不能为空"));
        verify(tSampExportMapper, never()).insert(any());
    }

    @Test
    void 导出_结束日期为空_拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampExportService.exportLedger(day("2026-09-01"), null, null));
        assertTrue(ex.getMessage().contains("结束日期不能为空"));
        verify(tSampExportMapper, never()).insert(any());
    }

    @Test
    void 导出_开始晚于结束_拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampExportService.exportLedger(day("2026-09-10"), day("2026-09-01"), null));
        assertTrue(ex.getMessage().contains("不能晚于结束日期"));
        verify(tSampExportMapper, never()).insert(any());
    }

    @Test
    void 导出_未登录_拒绝() {
        when(subject.getPrincipal()).thenReturn(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), null));
        assertTrue(ex.getMessage().contains("无法确定导出人"));
        verify(tSampExportMapper, never()).insert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 导出_检验类型与起止日期_拼入查询条件() {
        when(tSampLotMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampExportMapper.insert(any())).thenReturn(1);
        tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), "出厂");
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampLotMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        // 检验类型过滤不能丢，时间范围按报检日期
        assertTrue(sqlSegment.contains("inspect_type"));
        assertTrue(sqlSegment.contains("apply_date"));
        assertTrue(sqlSegment.contains("del_flag"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 导出_结束日期含当天_上界取次日零点() {
        when(tSampLotMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampExportMapper.insert(any())).thenReturn(1);
        // 选 03-01 ~ 03-31：03-31 当天的批必须落在范围内（历史上按结束日 00:00 开区间漏掉过）
        tSampExportService.exportLedger(day("2026-03-01"), day("2026-03-31"), null);
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampLotMapper).selectList(captor.capture());
        Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        // 下界 = 开始日 00:00，上界 = 结束日次日 00:00（不含），即 03-31 全天 < 04-01 00:00
        assertTrue(params.containsValue(Timestamp.valueOf("2026-03-01 00:00:00")));
        assertTrue(params.containsValue(Timestamp.valueOf("2026-04-01 00:00:00")));
        // 结束日本身的 00:00 不能直接当上界，否则结束日当天的批全部被漏掉
        assertFalse(params.containsValue(Timestamp.valueOf("2026-03-31 00:00:00")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 导出_检验类型为空_不按类型过滤() {
        when(tSampLotMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampExportMapper.insert(any())).thenReturn(1);
        tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), null);
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampLotMapper).selectList(captor.capture());
        assertFalse(captor.getValue().getSqlSegment().contains("inspect_type"));
    }

    @Test
    void 导出_正常_记录落库_文件带BOM中文表头() throws Exception {
        List<TSampLot> lots = Arrays.asList(lot("LOT-001", "示例产品甲"), lot("LOT-002", "示例产品乙"));
        when(tSampLotMapper.selectList(any())).thenReturn(lots);
        when(tSampExportMapper.insert(any())).thenReturn(1);
        TSampExport record = tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), "出厂");
        // 记录字段：批次号后端生成、条数、导出人取当前登录人、导出时间、fileName 为纯文件名
        assertTrue(record.getBatchNo().startsWith("EXP-"));
        assertEquals(2, record.getExportCount());
        assertEquals("zhangsan", record.getExportBy());
        assertNotNull(record.getExportTime());
        assertTrue(record.getFileName().startsWith("检验台账_"));
        assertTrue(record.getFileName().endsWith(".csv"));
        assertFalse(record.getFileName().contains("/"));
        assertFalse(record.getFileName().contains("\\"));
        assertEquals(0, record.getDelFlag());
        verify(tSampExportMapper).insert(any());
        // 文件落在导出目录，首三字节为 UTF-8 BOM（Excel 打开中文不乱码的关键）
        File file = new File(new File(tempDir, "sampExport"), record.getFileName());
        assertTrue(file.exists());
        byte[] bytes = Files.readAllBytes(file.toPath());
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        String[] lines = content.split("\r\n");
        assertEquals(3, lines.length);
        assertEquals("检验批号,产品,批量,抽样方案,应抽样本量,不合格数,合格率,判定结论,质量等级", lines[0]);
        assertTrue(lines[1].startsWith("LOT-001,示例产品甲,100,字码E/AQL2.50,13,1,92.31%,合格,一等品"));
    }

    @Test
    void 导出_无数据_文件只有表头_记录条数为0() throws Exception {
        when(tSampLotMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tSampExportMapper.insert(any())).thenReturn(1);
        TSampExport record = tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), null);
        assertEquals(0, record.getExportCount());
        File file = new File(new File(tempDir, "sampExport"), record.getFileName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertEquals("检验批号,产品,批量,抽样方案,应抽样本量,不合格数,合格率,判定结论,质量等级\r\n", content);
    }

    @Test
    void 导出_产品名含逗号_字段加引号转义() throws Exception {
        when(tSampLotMapper.selectList(any())).thenReturn(Collections.singletonList(lot("LOT-003", "产品,甲")));
        when(tSampExportMapper.insert(any())).thenReturn(1);
        TSampExport record = tSampExportService.exportLedger(day("2026-09-01"), day("2026-09-30"), null);
        File file = new File(new File(tempDir, "sampExport"), record.getFileName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        String[] lines = content.split("\r\n");
        // 含逗号的字段必须加引号，否则一行的列会被顶错位
        assertTrue(lines[1].startsWith("LOT-003,\"产品,甲\",100,"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 列表查询_过滤已删除() {
        tSampExportService.selectTSampExportList(new QueryWrapper<TSampExport>());
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tSampExportMapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("del_flag"));
    }
}
