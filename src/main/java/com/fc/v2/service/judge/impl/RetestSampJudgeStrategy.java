package com.fc.v2.service.judge.impl;

import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeStrategy;
import com.fc.v2.service.judge.SampJudgeType;
import org.springframework.stereotype.Component;

/**
 * 复检判定：复检合格检验批改判合格，复检不合格维持不合格；
 * 复检只改结论，不引起检验批状态流转
 *
 * @author fuce
 * @date 2026-09-13
 */
@Component
public class RetestSampJudgeStrategy implements SampJudgeStrategy {

    /** 复检结果：合格 */
    private static final int RESULT_PASS = 0;

    /** 判定结论：合格 */
    private static final String CONCLUDE_PASS = "合格";
    /** 判定结论：不合格 */
    private static final String CONCLUDE_FAIL = "不合格";

    @Override
    public SampJudgeType supportType() {
        return SampJudgeType.RETEST;
    }

    @Override
    public SampJudgeResult judge(SampJudgeContext context) {
        String conclude = Integer.valueOf(RESULT_PASS).equals(context.getRetestResult())
                ? CONCLUDE_PASS : CONCLUDE_FAIL;
        return new SampJudgeResult(null, conclude);
    }
}
