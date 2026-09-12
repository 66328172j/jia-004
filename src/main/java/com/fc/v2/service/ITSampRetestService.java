package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fc.v2.model.auto.TSampRetest;

/**
 * 检验批复检记录Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampRetestService extends IService<TSampRetest> {

    /**
     * 查询复检记录
     *
     * @param id 主键
     * @return 复检记录
     */
    public TSampRetest selectTSampRetestById(Long id);

    /**
     * 查询复检记录列表
     *
     * @param queryWrapper 查询条件
     * @return 复检记录集合
     */
    public List<TSampRetest> selectTSampRetestList(Wrapper<TSampRetest> queryWrapper);

    /**
     * 发起复检：生成复检单，状态置为待复检
     *
     * @param tSampRetest 复检记录
     * @return 结果
     */
    public int insertTSampRetest(TSampRetest tSampRetest);

    /**
     * 修改复检记录
     *
     * @param tSampRetest 复检记录
     * @return 结果
     */
    public int updateTSampRetest(TSampRetest tSampRetest);

    /**
     * 录入复检结果并回写检验批判定
     *
     * @param id 复检单主键
     * @param retestResult 复检判定 0合格 1不合格
     * @return 结果
     */
    public int finishTSampRetest(Long id, Integer retestResult);

    /**
     * 作废复检单
     *
     * @param id 复检单主键
     * @return 结果
     */
    public int cancelTSampRetest(Long id);

    /**
     * 批量删除复检记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSampRetestByIds(String ids);

    /**
     * 删除复检记录
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteTSampRetestById(Long id);
}
