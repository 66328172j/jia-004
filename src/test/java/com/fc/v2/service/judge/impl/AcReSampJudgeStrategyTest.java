package com.fc.v2.service.judge.impl;

import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 一次抽样判定单测：d≤Ac合格、d≥Re不合格、Ac/Re间隙停在待判定
 */
class AcReSampJudgeStrategyTest {

    private final AcReSampJudgeStrategy strategy = new AcReSampJudgeStrategy();

    private SampJudgeContext context(Integer defect, Integer accept, Integer reject) {
        SampJudgeContext context = new SampJudgeContext();
        context.setDefect(defect);
        context.setAccept(accept);
        context.setReject(reject);
        return context;
    }

    @Test
    void 判定口径为一次抽样() {
        assertEquals(SampJudgeType.AC_RE, strategy.supportType());
    }

    @Test
    void 不合格数等于接收数_判定合格并流转已判定() {
        SampJudgeResult result = strategy.judge(context(2, 2, 3));
        assertEquals(Integer.valueOf(3), result.getStatus());
        assertEquals("合格", result.getConclude());
    }

    @Test
    void 不合格数等于拒收数_判定不合格并流转已判定() {
        SampJudgeResult result = strategy.judge(context(3, 2, 3));
        assertEquals(Integer.valueOf(3), result.getStatus());
        assertEquals("不合格", result.getConclude());
    }

    @Test
    void 不合格数落在AcRe间隙_停在待判定且无结论() {
        SampJudgeResult result = strategy.judge(context(2, 1, 3));
        assertEquals(Integer.valueOf(2), result.getStatus());
        assertNull(result.getConclude());
    }

    @Test
    void 接收数为空时跳过接收判定_达到拒收数判不合格() {
        SampJudgeResult result = strategy.judge(context(3, null, 3));
        assertEquals(Integer.valueOf(3), result.getStatus());
        assertEquals("不合格", result.getConclude());
    }

    @Test
    void 拒收数为空时跳过拒收判定_超过接收数仍停在待判定() {
        SampJudgeResult result = strategy.judge(context(5, 2, null));
        assertEquals(Integer.valueOf(2), result.getStatus());
        assertNull(result.getConclude());
    }

    @Test
    void 接收数拒收数均为空_停在待判定() {
        SampJudgeResult result = strategy.judge(context(1, null, null));
        assertEquals(Integer.valueOf(2), result.getStatus());
        assertNull(result.getConclude());
    }

    @Test
    void 加严方案接收数为零_无不合格样本判定合格() {
        SampJudgeResult result = strategy.judge(context(0, 0, 1));
        assertEquals(Integer.valueOf(3), result.getStatus());
        assertEquals("合格", result.getConclude());
    }
}
