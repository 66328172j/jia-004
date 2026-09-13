package com.fc.v2.service.judge;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 判定编排入口：按判定口径分发到对应策略，Service 只负责组判定输入与结果落库
 *
 * @author fuce
 * @date 2026-09-13
 */
@Component
public class SampJudgeExecutor {

    private final Map<SampJudgeType, SampJudgeStrategy> strategies =
            new EnumMap<SampJudgeType, SampJudgeStrategy>(SampJudgeType.class);

    @Autowired
    public SampJudgeExecutor(List<SampJudgeStrategy> strategyList) {
        for (SampJudgeStrategy strategy : strategyList) {
            strategies.put(strategy.supportType(), strategy);
        }
    }

    /**
     * 按判定口径执行判定
     *
     * @param type    判定口径
     * @param context 判定输入
     * @return 判定输出
     */
    public SampJudgeResult judge(SampJudgeType type, SampJudgeContext context) {
        SampJudgeStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的判定口径:" + type);
        }
        return strategy.judge(context);
    }
}
