package com.fc.v2.service.judge.impl;

import com.fc.v2.service.judge.SampJudgeContext;
import com.fc.v2.service.judge.SampJudgeResult;
import com.fc.v2.service.judge.SampJudgeStrategy;
import com.fc.v2.service.judge.SampJudgeType;
import org.springframework.stereotype.Component;

/**
 * 一次抽样判定：不合格样本数 d ≤ Ac 接收（合格），d ≥ Re 拒收（不合格）；
 * 落在 Ac/Re 间隙（个别加严方案）时保留待判定状态
 *
 * @author fuce
 * @date 2026-09-13
 */
@Component
public class AcReSampJudgeStrategy implements SampJudgeStrategy {

    /** 检验批状态：待判定 */
    private static final int STATUS_PENDING_JUDGE = 2;
    /** 检验批状态：已判定 */
    private static final int STATUS_JUDGED = 3;

    /** 判定结论：合格 */
    private static final String CONCLUDE_PASS = "合格";
    /** 判定结论：不合格 */
    private static final String CONCLUDE_FAIL = "不合格";

    @Override
    public SampJudgeType supportType() {
        return SampJudgeType.AC_RE;
    }

    @Override
    public SampJudgeResult judge(SampJudgeContext context) {
        int defect = context.getDefect() == null ? 0 : context.getDefect();
        Integer accept = context.getAccept();
        Integer reject = context.getReject();
        if (accept != null && defect <= accept) {
            return new SampJudgeResult(STATUS_JUDGED, CONCLUDE_PASS);
        }
        if (reject != null && defect >= reject) {
            return new SampJudgeResult(STATUS_JUDGED, CONCLUDE_FAIL);
        }
        // Ac/Re 间隙：暂不结论，停在待判定
        return new SampJudgeResult(STATUS_PENDING_JUDGE, null);
    }
}
