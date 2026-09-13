package com.fc.v2.service.judge;

import com.fc.v2.service.judge.impl.AcReSampJudgeStrategy;
import com.fc.v2.service.judge.impl.RetestSampJudgeStrategy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 判定编排单测：按判定口径分发到对应策略，未注册口径明确报错
 */
class SampJudgeExecutorTest {

    private final SampJudgeExecutor executor = new SampJudgeExecutor(
            Arrays.asList(new AcReSampJudgeStrategy(), new RetestSampJudgeStrategy()));

    @Test
    void 一次抽样口径分发到AcRe策略() {
        SampJudgeContext context = new SampJudgeContext();
        context.setDefect(0);
        context.setAccept(0);
        context.setReject(1);
        SampJudgeResult result = executor.judge(SampJudgeType.AC_RE, context);
        assertEquals("合格", result.getConclude());
    }

    @Test
    void 复检口径分发到复检策略() {
        SampJudgeContext context = new SampJudgeContext();
        context.setRetestResult(0);
        SampJudgeResult result = executor.judge(SampJudgeType.RETEST, context);
        assertEquals("合格", result.getConclude());
    }

    @Test
    void 口径未注册时明确报错() {
        SampJudgeExecutor emptyExecutor =
                new SampJudgeExecutor(Collections.<SampJudgeStrategy>emptyList());
        assertThrows(IllegalArgumentException.class,
                () -> emptyExecutor.judge(SampJudgeType.AC_RE, new SampJudgeContext()));
    }
}
