package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TSampJobLogMapper;
import com.fc.v2.mapper.auto.TSampJobMapper;
import com.fc.v2.mapper.auto.TSampUrgeMapper;
import com.fc.v2.model.auto.TSampJob;
import com.fc.v2.model.auto.TSampJobLog;
import com.fc.v2.model.auto.TSampLot;
import com.fc.v2.model.auto.TSampUrge;
import com.fc.v2.service.ITSampLotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超期扫描任务单测：停用任务不执行、超期批自动生成催办、
 * 同日同批不重复催办、单批失败计入失败条数、不中断后续批且不影响日志落库、
 * 并发重复触发被拒绝且执行完成后锁正常释放、
 * 成功失败都落任务日志（扫描异常/Error/日志落库失败均不留"无记录"缺口）
 */
class TSampJobServiceImplTest {

    // 类里有多个 BaseMapper 类型的 mock，Mockito 按字段名消歧，
    // 必须命名为 baseMapper 才会注入到 ServiceImpl 的 baseMapper 字段
    @Mock
    private TSampJobMapper baseMapper;

    @Mock
    private ITSampLotService tSampLotService;

    @Mock
    private TSampUrgeMapper tSampUrgeMapper;

    @Mock
    private TSampJobLogMapper tSampJobLogMapper;

    @InjectMocks
    private TSampJobServiceImpl tSampJobService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private TSampJob job(Integer status) {
        TSampJob job = new TSampJob();
        job.setId("job1");
        job.setJobName("检验批超期催办扫描");
        job.setStatus(status);
        job.setDelFlag(0);
        return job;
    }

    private TSampLot overdueLot(int overdueDays) {
        TSampLot lot = new TSampLot();
        lot.setId(1L);
        lot.setLotNo("LOT-001");
        lot.setStatus(2);
        lot.setOverdueDays(overdueDays);
        return lot;
    }

    /**
     * 公共 mock：任务启用，扫描到一批超期 5 天，今天尚未催办过
     */
    private void mockOneOverdueLot() {
        when(baseMapper.selectById("job1")).thenReturn(job(0));
        List<TSampLot> lots = new ArrayList<TSampLot>();
        lots.add(overdueLot(5));
        when(tSampLotService.selectTSampLotList(any())).thenReturn(lots);
        when(tSampUrgeMapper.selectCount(any())).thenReturn(0);
        when(tSampUrgeMapper.selectList(any())).thenReturn(Collections.<TSampUrge>emptyList());
    }

    @Test
    void disabledJobReturnsNullAndDoesNothing() {
        when(baseMapper.selectById("job1")).thenReturn(job(1));

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNull(result);
        verify(tSampUrgeMapper, never()).insert(any(TSampUrge.class));
        verify(tSampJobLogMapper, never()).insert(any(TSampJobLog.class));
    }

    @Test
    void missingJobReturnsNull() {
        when(baseMapper.selectById("job1")).thenReturn(null);

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNull(result);
        verify(tSampJobLogMapper, never()).insert(any(TSampJobLog.class));
    }

    @Test
    void overdueLotGeneratesUrgeAndSuccessLog() {
        mockOneOverdueLot();

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNotNull(result);
        // 生成一条来源为定时扫描的待处理催办
        ArgumentCaptor<TSampUrge> urgeCaptor = ArgumentCaptor.forClass(TSampUrge.class);
        verify(tSampUrgeMapper).insert(urgeCaptor.capture());
        TSampUrge urge = urgeCaptor.getValue();
        assertEquals(Long.valueOf(1L), urge.getLotId());
        assertEquals(Integer.valueOf(1), urge.getSource());
        assertEquals(Integer.valueOf(0), urge.getStatus());
        assertEquals(Integer.valueOf(1), urge.getUrgeCount());
        assertEquals(Integer.valueOf(5), urge.getOverdueDays());
        assertNotNull(urge.getUrgeNo());
        // 日志：扫描1批、成功1条、失败0条、执行成功
        assertEquals(Integer.valueOf(1), result.getScanCount());
        assertEquals(Integer.valueOf(1), result.getUrgeCount());
        assertEquals(Integer.valueOf(0), result.getFailCount());
        assertEquals(Integer.valueOf(0), result.getStatus());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        verify(tSampJobLogMapper).insert(any(TSampJobLog.class));
    }

    @Test
    void urgedTodayLotIsSkipped() {
        mockOneOverdueLot();
        // 今天已催办过该批
        when(tSampUrgeMapper.selectCount(any())).thenReturn(1);

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNotNull(result);
        verify(tSampUrgeMapper, never()).insert(any(TSampUrge.class));
        assertEquals(Integer.valueOf(1), result.getScanCount());
        assertEquals(Integer.valueOf(0), result.getUrgeCount());
        assertEquals(Integer.valueOf(0), result.getStatus());
    }

    @Test
    void notOverdueLotIsSkipped() {
        when(baseMapper.selectById("job1")).thenReturn(job(0));
        List<TSampLot> lots = new ArrayList<TSampLot>();
        lots.add(overdueLot(0));
        when(tSampLotService.selectTSampLotList(any())).thenReturn(lots);

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNotNull(result);
        verify(tSampUrgeMapper, never()).insert(any(TSampUrge.class));
        assertEquals(Integer.valueOf(1), result.getScanCount());
        assertEquals(Integer.valueOf(0), result.getUrgeCount());
    }

    @Test
    void urgeInsertFailureCountsAsFailAndStillLogs() {
        mockOneOverdueLot();
        when(tSampUrgeMapper.insert(any(TSampUrge.class))).thenThrow(new RuntimeException("唯一键冲突"));

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getScanCount());
        assertEquals(Integer.valueOf(0), result.getUrgeCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertEquals(Integer.valueOf(1), result.getStatus());
        assertNotNull(result.getErrorMsg());
        verify(tSampJobLogMapper).insert(any(TSampJobLog.class));
    }

    @Test
    void singleLotCheckFailureDoesNotAbortRemainingLots() {
        when(baseMapper.selectById("job1")).thenReturn(job(0));
        // 三批超期，中间一批的当日去重检查抛异常
        List<TSampLot> lots = new ArrayList<TSampLot>();
        lots.add(overdueLot(5));
        TSampLot badLot = overdueLot(3);
        badLot.setId(2L);
        badLot.setLotNo("LOT-002");
        lots.add(badLot);
        TSampLot thirdLot = overdueLot(7);
        thirdLot.setId(3L);
        thirdLot.setLotNo("LOT-003");
        lots.add(thirdLot);
        when(tSampLotService.selectTSampLotList(any())).thenReturn(lots);
        // 第一批检查正常，第二批检查时抛异常，第三批恢复正常
        when(tSampUrgeMapper.selectCount(any()))
                .thenReturn(0)
                .thenThrow(new RuntimeException("查询超时"))
                .thenReturn(0);
        when(tSampUrgeMapper.selectList(any())).thenReturn(Collections.<TSampUrge>emptyList());

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        // 中间一批失败不拖垮整批：前后两批照常生成催办，坏批计入失败条数
        assertNotNull(result);
        assertEquals(Integer.valueOf(3), result.getScanCount());
        assertEquals(Integer.valueOf(2), result.getUrgeCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertEquals(Integer.valueOf(1), result.getStatus());
        assertTrue(result.getErrorMsg().contains("LOT-002"));
        ArgumentCaptor<TSampUrge> urgeCaptor = ArgumentCaptor.forClass(TSampUrge.class);
        verify(tSampUrgeMapper, times(2)).insert(urgeCaptor.capture());
        List<TSampUrge> urges = urgeCaptor.getAllValues();
        assertEquals(Long.valueOf(1L), urges.get(0).getLotId());
        assertEquals(Long.valueOf(3L), urges.get(1).getLotId());
        verify(tSampJobLogMapper).insert(any(TSampJobLog.class));
    }

    @Test
    void scanFailureStillWritesFailureLog() {
        when(baseMapper.selectById("job1")).thenReturn(job(0));
        when(tSampLotService.selectTSampLotList(any())).thenThrow(new RuntimeException("数据库连接失败"));

        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        // 扫描中途报错：方法正常返回，且落一条 status=1 的失败日志，页面上能看出是"跑了失败"
        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getStatus());
        assertNotNull(result.getErrorMsg());
        assertTrue(result.getErrorMsg().contains("扫描超期检验批异常"));
        ArgumentCaptor<TSampJobLog> logCaptor = ArgumentCaptor.forClass(TSampJobLog.class);
        verify(tSampJobLogMapper).insert(logCaptor.capture());
        TSampJobLog persisted = logCaptor.getValue();
        assertEquals(Integer.valueOf(1), persisted.getStatus());
        assertNotNull(persisted.getStartTime());
        assertNotNull(persisted.getEndTime());
    }

    @Test
    void logInsertFailureDoesNotBreakRun() {
        mockOneOverdueLot();
        when(tSampJobLogMapper.insert(any(TSampJobLog.class))).thenThrow(new RuntimeException("日志表写入失败"));

        // 日志落库失败不再上抛：任务执行结果照常返回，失败只记应用日志
        TSampJobLog result = tSampJobService.runOverdueScan("job1");

        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getUrgeCount());
        assertEquals(Integer.valueOf(0), result.getStatus());
    }

    @Test
    void errorDuringScanWritesFailureLogAndRethrows() {
        mockOneOverdueLot();
        when(tSampUrgeMapper.insert(any(TSampUrge.class))).thenThrow(new StackOverflowError("mock"));

        // Error 不吞：落一条失败日志后原样上抛
        try {
            tSampJobService.runOverdueScan("job1");
            fail("Error 应记录日志后原样上抛");
        } catch (StackOverflowError expected) {
            // 预期上抛
        }
        ArgumentCaptor<TSampJobLog> logCaptor = ArgumentCaptor.forClass(TSampJobLog.class);
        verify(tSampJobLogMapper).insert(logCaptor.capture());
        assertEquals(Integer.valueOf(1), logCaptor.getValue().getStatus());
        assertTrue(logCaptor.getValue().getErrorMsg().contains("扫描超期检验批异常"));

        // Error 之后锁已释放，后续触发不受影响
        when(tSampUrgeMapper.insert(any(TSampUrge.class))).thenReturn(1);
        TSampJobLog second = tSampJobService.runOverdueScan("job1");
        assertNotNull(second);
        assertEquals(Integer.valueOf(0), second.getStatus());
    }

    @Test
    void concurrentRunIsRejected() throws Exception {
        mockOneOverdueLot();
        // 让第一个执行卡在催办插入里，模拟"任务执行中"
        CountDownLatch insertStarted = new CountDownLatch(1);
        CountDownLatch releaseInsert = new CountDownLatch(1);
        when(tSampUrgeMapper.insert(any(TSampUrge.class))).thenAnswer(invocation -> {
            insertStarted.countDown();
            releaseInsert.await(5, TimeUnit.SECONDS);
            return 1;
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<TSampJobLog> first = executor.submit(() -> {
                return tSampJobService.runOverdueScan("job1");
            });
            assertTrue(insertStarted.await(5, TimeUnit.SECONDS));

            // 执行进行中再次触发（如手动连点）：应被拒绝，不再扫描、不落日志
            try {
                tSampJobService.runOverdueScan("job1");
                fail("执行中重复触发应抛出 IllegalStateException");
            } catch (IllegalStateException e) {
                assertTrue(e.getMessage().contains("正在执行中"));
            }

            releaseInsert.countDown();
            TSampJobLog firstLog = first.get(5, TimeUnit.SECONDS);
            assertNotNull(firstLog);
            assertEquals(Integer.valueOf(1), firstLog.getUrgeCount());
        } finally {
            releaseInsert.countDown();
            executor.shutdownNow();
        }
        // 两次触发只真正执行了一次：只插一条催办、只落一条日志
        verify(tSampUrgeMapper, times(1)).insert(any(TSampUrge.class));
        verify(tSampJobLogMapper, times(1)).insert(any(TSampJobLog.class));
    }

    @Test
    void lockReleasedAfterRunSoLaterTriggerRunsNormally() {
        // 第一次执行完成后锁应释放，后续（串行）触发不会被误判为"执行中"
        mockOneOverdueLot();
        TSampJobLog first = tSampJobService.runOverdueScan("job1");
        assertNotNull(first);
        assertEquals(Integer.valueOf(1), first.getUrgeCount());

        // 当天该批已催办过，第二次触发正常执行但按同日去重跳过
        when(tSampUrgeMapper.selectCount(any())).thenReturn(1);
        TSampJobLog second = tSampJobService.runOverdueScan("job1");
        assertNotNull(second);
        assertEquals(Integer.valueOf(0), second.getUrgeCount());
        verify(tSampUrgeMapper, times(1)).insert(any(TSampUrge.class));
    }
}
