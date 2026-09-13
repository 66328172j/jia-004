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
     * 口径：只统计未删除且已判定（已判定/已关闭）的检验批；
     * 合格率 = 合格批数 / 已判定批数；
     * 一次检验通过率 = 一次判定合格（无待复检/已完成复检单）的批数 / 已判定批数，
     * 复检后改判合格的批不计入一次通过。
     *
     * @param beginTime 开始时间（含，按报检日期算），null 不限制
     * @param endTime   结束时间（不含），null 不限制
     * @param productId 产品ID，null 统计全部产品
     * @return total 批次数、passCount、failCount、passRate、firstPassRate、rows 各产品统计行
     */
    Map<String, Object> statisticsByProduct(Date beginTime, Date endTime, Long productId);

    /**
     * 按月份维度统计检验质量趋势（月份按时间先后升序）
     * 入参给了完整时间范围时，没有数据的月份补零，保证趋势连续
     *
     * @param beginTime 开始时间（含），null 不限制
     * @param endTime   结束时间（不含），null 不限制
     * @param productId 产品ID，null 统计全部产品
     * @return month(yyyy-MM)、lotCount、passCount、failCount、passRate、firstPassRate
     */
    List<Map<String, Object>> trendByMonth(Date beginTime, Date endTime, Long productId);
}
