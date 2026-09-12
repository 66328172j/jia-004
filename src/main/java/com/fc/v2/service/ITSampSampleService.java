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
     * 查询样本检测记录列表
     *
     * @param queryWrapper 查询条件
     * @return 样本检测记录集合
     */
    public List<TSampSample> selectTSampSampleList(Wrapper<TSampSample> queryWrapper);

    /**
     * 新增样本检测记录
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    public int insertTSampSample(TSampSample tSampSample);

    /**
     * 修改样本检测记录
     *
     * @param tSampSample 样本检测记录
     * @return 结果
     */
    public int updateTSampSample(TSampSample tSampSample);

    /**
     * 汇总检验批的不合格样本数与合格率，并在样本登记满额时自动判定
     *
     * @param lotId 检验批ID
     * @return 结果
     */
    public int summaryAndJudgeLot(Long lotId);

    /**
     * 按检验批判定：不合格样本数与抽样方案拒收数比较，回写结论与状态
     *
     * @param lotId 检验批ID
     * @return 结果
     */
    public int judgeLot(Long lotId);

    /**
     * 批量删除样本检测记录
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
