package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampSample;

/**
 * 检验样本检测记录Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampSampleService extends IService<TSampSample> {

    /**
     * 查询样本检测记录
     *
     * @param id 主键
     * @return 样本检测记录
     */
    public TSampSample selectTSampSampleById(Long id);

    /**
     * 查询样本检测记录列表（按检验批、样本编号排序）
     *
     * @param queryWrapper 查询条件
     * @return 样本检测记录集合
     */
    public List<TSampSample> selectTSampSampleList(Wrapper<TSampSample> queryWrapper);

    /**
     * 查询某检验批下已录入的样本编号
     *
     * @param lotId 检验批ID
     * @return 样本编号集合
     */
    public List<String> selectRecordedSampleNos(Long lotId);

    /**
     * 新增样本检测记录：样本编号按检验批应抽样本量生成（S01..Sn），检测人/检测时间由系统记录，
     * 录入后自动汇总检验批不合格数与合格率，样本录满时自动判定并流转到已判定
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    public int insertTSampSample(TSampSample tSampSample);

    /**
     * 修改样本检测记录（样本编号与检测人/检测时间不允许改动），修改后重新汇总判定检验批
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    public int updateTSampSample(TSampSample tSampSample);

    /**
     * 批量删除样本检测记录，删除后重新汇总检验批
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSampSampleByIds(String ids);

    /**
     * 删除样本检测记录
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteTSampSampleById(Long id);
}
