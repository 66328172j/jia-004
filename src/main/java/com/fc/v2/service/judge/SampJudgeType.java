package com.fc.v2.service.judge;

/**
 * 检验批判定口径：每种口径对应一个独立的判定策略
 *
 * @author fuce
 * @date 2026-09-13
 */
public enum SampJudgeType {

    /** 一次抽样判定：按抽样方案的接收数Ac/拒收数Re对不合格样本数作结论 */
    AC_RE,

    /** 复检判定：按复检结果对检验批结论改判 */
    RETEST
}
