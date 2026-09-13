package com.fc.v2.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TSampQueryLog;

/**
 * 移动端扫码查询Service接口
 *
 * 对外查询（客户扫码）只返回脱敏字段：批号、产品编码、产品名称、检验状态、判定结论、质量等级；
 * 抽样方案（AQL、样本量、接收数、拒收数）与内部统计字段不得出现在对外结果中。
 * 每次查询都要落一条查询记录（批号、命中与否、来源、查询时间），查询失败不影响主流程。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampScanService {

    /**
     * 扫码查询检验批
     *
     * @param lotNo  批号
     * @param source 查询来源（如 APP / WECHAT / WEB）
     * @return hit 是否命中（Boolean）、message 提示语（未命中时给出明确提示）、
     *         data 对外脱敏视图（未命中为 null）
     */
    Map<String, Object> scanQuery(String lotNo, String source);

    /**
     * 查询移动端查询记录列表
     *
     * @param queryWrapper 查询条件（按批号模糊查等）
     * @return 查询记录集合
     */
    List<TSampQueryLog> selectQueryLogList(Wrapper<TSampQueryLog> queryWrapper);
}
