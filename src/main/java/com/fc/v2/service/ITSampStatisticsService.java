package com.fc.v2.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 质量统计Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampStatisticsService {

    /**
     * 按产品维度统计检验质量
     * 口径：只统计未删除的检验批；合格批数 / 已判定批数 = 合格率；
     * 一次检验通过率只看没有复检记录的已判定批。
     *
     * @param beginTime 开始时间（含，按报检日期算）
     * @param endTime   结束时间（该时间所在月份整月都算在内）
     * @return total 批次数、rows 各产品统计行、avgPassRate 平均合格率
     */
    Map<String, Object> statisticsByProduct(Date beginTime, Date endTime);

    /**
     * 按月份维度统计检验质量趋势（月份按时间先后升序）
     *
     * @param beginTime 开始时间（含）
     * @param endTime   结束时间（该时间所在月份整月都算在内）
     * @return month(yyyy-MM)、lotCount、passCount、failCount、passRate、firstPassRate
     */
    List<Map<String, Object>> trendByMonth(Date beginTime, Date endTime);
}
