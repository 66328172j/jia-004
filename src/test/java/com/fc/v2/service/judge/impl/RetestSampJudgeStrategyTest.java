package com.fc.v2.service.judge.impl;

import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 复检判定单测：复检合格改判合格、复检不合格维持不合格、不引起状态流转
 */
class RetestSampJudgeStrategyTest {

    private final RetestSampJudgeStrategy strategy = new RetestSampJudgeStrategy();

    private SampJudgeContext context(Integer retestResult) {
        SampJudgeContext context = new SampJudgeContext();
        context.setRetestResult(retestResult);
        return context;
    }

    @Test
    void 判定口径为复检() {
        assertEquals(SampJudgeType.RETEST, strategy.supportType());
    }

    @Test
    void 复检合格_改判合格且不流转状态() {
        SampJudgeResult result = strategy.judge(context(0));
        assertNull(result.getStatus());
        assertEquals("合格", result.getConclude());
    }

    @Test
    void 复检不合格_维持不合格且不流转状态() {
        SampJudgeResult result = strategy.judge(context(1));
        assertNull(result.getStatus());
        assertEquals("不合格", result.getConclude());
    }
}
