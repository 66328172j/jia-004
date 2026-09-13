package com.fc.v2.service.judge;

/**
 * 检验批判定策略：一种判定口径一个实现，判定规则调整只动对应策略
 *
 * @author fuce
 * @date 2026-09-13
 */
public interface SampJudgeStrategy {

    /**
     * 支持的判定口径
     *
     * @return 判定口径
     */
    SampJudgeType supportType();

    /**
     * 按口径判定
     *
     * @param context 判定输入
     * @return 判定输出
     */
    SampJudgeResult judge(SampJudgeContext context);
}
