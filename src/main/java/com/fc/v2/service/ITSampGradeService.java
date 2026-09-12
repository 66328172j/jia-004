package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import com.fc.v2.model.auto.TSampGrade;
import com.fc.v2.model.auto.TSampGradeRule;

/**
 * 质量定等Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITSampGradeService extends IService<TSampGrade> {

    /**
     * 查询定等规则
     *
     * @param id 主键
     * @return 定等规则
     */
    public TSampGradeRule selectRuleById(String id);

    /**
     * 查询定等规则列表
     *
     * @param queryWrapper 查询条件
     * @return 定等规则集合
     */
    public List<TSampGradeRule> selectRuleList(Wrapper<TSampGradeRule> queryWrapper);

    /**
     * 新增定等规则
     *
     * @param rule 定等规则
     * @return 结果
     */
    public int insertRule(TSampGradeRule rule);

    /**
     * 修改定等规则
     *
     * @param rule 定等规则
     * @return 结果
     */
    public int updateRule(TSampGradeRule rule);

    /**
     * 对检验批自动定等：按合格率与不合格项数匹配规则档位，回写检验批等级并落定等记录
     *
     * @param lotId 检验批ID
     * @return 定等记录
     */
    public TSampGrade gradeLot(Long lotId);

    /**
     * 查询某检验批的定等记录
     *
     * @param lotId 检验批ID
     * @return 定等记录
     */
    public TSampGrade selectGradeByLotId(Long lotId);

    /**
     * 查询定等记录列表
     *
     * @param queryWrapper 查询条件
     * @return 定等记录集合
     */
    public List<TSampGrade> selectTSampGradeList(Wrapper<TSampGrade> queryWrapper);
}
