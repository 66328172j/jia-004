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
     * 查询复检单
     *
     * @param id 主键
     * @return 复检单
     */
    public TSampRetest selectTSampRetestById(Long id);

    /**
     * 查询复检单列表
     *
     * @param queryWrapper 查询条件
     * @return 复检单集合
     */
    public List<TSampRetest> selectTSampRetestList(Wrapper<TSampRetest> queryWrapper);

    /**
     * 发起复检：只有判定不合格的检验批可以发起，生成待复检单，
     * 复检样本量按检验批应抽样本量的两倍记录，原判定取检验批当前结论
     *
     * @param tSampRetest 复检单
     * @return 结果
     */
    public int insertTSampRetest(TSampRetest tSampRetest);

    /**
     * 录入复检结果：复检人取当前登录人、复检时间取当前时间，状态置为已完成；
     * 复检合格回写检验批为合格，复检不合格维持不合格
     *
     * @param id           复检单主键
     * @param retestResult 复检判定 0合格 1不合格
     * @return 结果
     */
    public int finishTSampRetest(Long id, Integer retestResult);

    /**
     * 作废复检单：只有待复检的复检单允许作废，作废后不再回写检验批
     *
     * @param id 复检单主键
     * @return 结果
     */
    public int cancelTSampRetest(Long id);

    /**
     * 批量删除复检单
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSampRetestByIds(String ids);

    /**
     * 删除复检单
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteTSampRetestById(Long id);
}
